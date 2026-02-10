
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PSNStandalone
{
    public static class Process
    {
        public final String name;
        public final int arrival;
        public final int burst;
        public final int priority;

        public int startTime;
        public int finishTime;
        public int waitingTime;
        public int turnaroundTime;

        public Process(String name, int arrival, int burst, int priority)
        {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
        }
    }

    public static class Event
    {
        public final String process;
        public final int start;
        public final int finish;

        public Event(String process, int start, int finish)
        {
            this.process = process;
            this.start = start;
            this.finish = finish;
        }
    }

    public static class Result
    {
        public final List<Event> timeline;
        public final List<Process> processes;
        public final double averageWaitingTime;
        public final double averageTurnaroundTime;

        public Result(List<Event> timeline, List<Process> processes)
        {
            this.timeline = timeline;
            this.processes = processes;

            double totalWaiting = 0.0;
            double totalTurnaround = 0.0;

            for (Process p : processes)
            {
                totalWaiting += p.waitingTime;
                totalTurnaround += p.turnaroundTime;
            }

            this.averageWaitingTime = totalWaiting / processes.size();
            this.averageTurnaroundTime = totalTurnaround / processes.size();
        }
    }

    public static Result schedule(List<Process> input)
    {
        List<Process> pending = new ArrayList();
        List<Process> completed = new ArrayList();
        List<Event> timeline = new ArrayList();

        for (Process p : input)
        {
            pending.add(new Process(p.name, p.arrival, p.burst, p.priority));
        }

        pending.sort(Comparator.comparingInt(p -> p.arrival));
        int time = pending.get(0).arrival;

        while (!pending.isEmpty())
        {
            List<Process> available = new ArrayList();

            for (Process p : pending)
            {
                if (p.arrival <= time)
                {
                    available.add(p);
                }
            }

            if (available.isEmpty())
            {
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : pending)
                {
                    if (p.arrival > time && p.arrival < nextArrival)
                    {
                        nextArrival = p.arrival;
                    }
                }
                if (nextArrival == Integer.MAX_VALUE)
                {
                    break;
                }
                time = nextArrival;
                continue;
            }

            available.sort((a, b) -> {
                if (a.priority == b.priority)
                {
                    return Integer.compare(a.arrival, b.arrival);
                }
                return Integer.compare(a.priority, b.priority);
            });

            Process current = available.get(0);
            int start = time;
            int finish = time + current.burst;

            current.startTime = start;
            current.finishTime = finish;
            current.waitingTime = start - current.arrival;
            current.turnaroundTime = finish - current.arrival;

            timeline.add(new Event(current.name, start, finish));
            time = finish;

            pending.remove(current);
            completed.add(current);
        }

        return new Result(timeline, completed);
    }

    public static void main(String[] args)
    {
        List<Process> processes = new ArrayList();
        processes.add(new Process("P1", 0, 10, 3));
        processes.add(new Process("P2", 1, 1, 4));
        processes.add(new Process("P3", 9, 1, 1));

        Result result = schedule(processes);

        System.out.println("Timeline:");
        for (Event e : result.timeline)
        {
            System.out.println(e.process + ": " + e.start + " -> " + e.finish);
        }

        System.out.println("\nProcess stats:");
        for (Process p : result.processes)
        {
            System.out.println(p.name + " WT=" + p.waitingTime + " TAT=" + p.turnaroundTime + " FinalPriority=" + p.priority);
        }

        System.out.println("\nAvg WT=" + result.averageWaitingTime);
        System.out.println("Avg TAT=" + result.averageTurnaroundTime);
    }
}
