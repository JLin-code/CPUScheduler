
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriorityNonPreemptiveAging extends CPUScheduler
{
    private int agingThreshold = 3;

    public void setAgingThreshold(int agingThreshold)
    {
        this.agingThreshold = Math.max(1, agingThreshold);
    }

    @Override
    public void process()
    {
        Collections.sort(this.getRows(), (Object o1, Object o2) -> {
            if (((Row) o1).getArrivalTime() == ((Row) o2).getArrivalTime())
            {
                return 0;
            }
            else if (((Row) o1).getArrivalTime() < ((Row) o2).getArrivalTime())
            {
                return -1;
            }
            else
            {
                return 1;
            }
        });

        List<Row> rows = Utility.deepCopy(this.getRows());
        Map<String, Integer> waitCounters = new HashMap<String, Integer>();
        int time = rows.get(0).getArrivalTime();

        while (!rows.isEmpty())
        {
            List<Row> availableRows = new ArrayList<Row>();

            for (Row row : rows)
            {
                if (row.getArrivalTime() <= time)
                {
                    availableRows.add(row);
                }
            }

            if (availableRows.isEmpty())
            {
                int nextArrival = Integer.MAX_VALUE;

                for (Row row : rows)
                {
                    if (row.getArrivalTime() > time && row.getArrivalTime() < nextArrival)
                    {
                        nextArrival = row.getArrivalTime();
                    }
                }

                if (nextArrival == Integer.MAX_VALUE)
                {
                    break;
                }

                time = nextArrival;
                continue;
            }

            for (Row row : availableRows)
            {
                int waited = waitCounters.getOrDefault(row.getProcessName(), 0);

                while (waited >= agingThreshold)
                {
                    row.setPriorityLevel(Math.max(1, row.getPriorityLevel() - 1));
                    waited -= agingThreshold;
                }

                waitCounters.put(row.getProcessName(), waited);
            }

            Collections.sort(availableRows, (Object o1, Object o2) -> {
                if (((Row) o1).getPriorityLevel() == ((Row) o2).getPriorityLevel())
                {
                    return 0;
                }
                else if (((Row) o1).getPriorityLevel() < ((Row) o2).getPriorityLevel())
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            });

            Row row = availableRows.get(0);
            int startTime = time;
            int finishTime = time + row.getBurstTime();
            this.getTimeline().add(new Event(row.getProcessName(), startTime, finishTime));
            time = finishTime;

            for (Row other : rows)
            {
                if (!other.getProcessName().equals(row.getProcessName()) && other.getArrivalTime() < finishTime)
                {
                    int waitStart = Math.max(startTime, other.getArrivalTime());
                    int increment = finishTime - waitStart;

                    if (increment > 0)
                    {
                        int currentWait = waitCounters.getOrDefault(other.getProcessName(), 0);
                        waitCounters.put(other.getProcessName(), currentWait + increment);
                    }
                }
            }

            waitCounters.remove(row.getProcessName());

            for (int i = 0; i < rows.size(); i++)
            {
                if (rows.get(i).getProcessName().equals(row.getProcessName()))
                {
                    rows.remove(i);
                    break;
                }
            }
        }

        for (Row row : this.getRows())
        {
            row.setWaitingTime(this.getEvent(row).getStartTime() - row.getArrivalTime());
            row.setTurnaroundTime(row.getWaitingTime() + row.getBurstTime());
        }
    }
}
