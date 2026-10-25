import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

/**
 * CPU Scheduling Simulator
 * Operating Systems Mini-Project
 *
 * Simulates FCFS, SJF (non-preemptive), Priority (non-preemptive) and Round Robin
 * scheduling, and shows a Gantt-style timeline with CT, TAT, WT and their averages.
 * In this program a smaller priority number means a higher priority.
 */

/** Stores one process and its calculated values. */
class Process {
    int index;          // position in the original input (used to break ties)
    String id;
    int arrivalTime;
    int burstTime;
    int priority;
    int remainingTime;
    int completionTime;
    int turnaroundTime;
    int waitingTime;

    Process(int index, String id, int arrivalTime, int burstTime, int priority) {
        this.index = index;
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }

    /** Each algorithm works on its own copy so the original input is never changed. */
    Process copy() {
        return new Process(index, id, arrivalTime, burstTime, priority);
    }

    void finish(int time) {
        completionTime = time;
        turnaroundTime = completionTime - arrivalTime;
        waitingTime = turnaroundTime - burstTime;
    }
}

/** One interval of the timeline. pid is "IDLE" when no process is running. */
class Segment {
    String pid;
    int start;
    int end;

    Segment(String pid, int start, int end) {
        this.pid = pid;
        this.start = start;
        this.end = end;
    }
}

/** Result of running one algorithm. */
class Result {
    String algorithmName;
    List<Process> processes;
    List<Segment> timeline;

    Result(String algorithmName, List<Process> processes, List<Segment> timeline) {
        this.algorithmName = algorithmName;
        this.processes = processes;
        this.timeline = timeline;
    }

    double averageWaitingTime() {
        double sum = 0;
        for (Process p : processes) sum += p.waitingTime;
        return sum / processes.size();
    }

    double averageTurnaroundTime() {
        double sum = 0;
        for (Process p : processes) sum += p.turnaroundTime;
        return sum / processes.size();
    }
}

public class CPUSchedulingSimulator {

    private static final Scanner SC = new Scanner(System.in);

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------

    /** Keeps asking until the user enters an integer between min and max. */
    static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = SC.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a whole number.");
            }
        }
    }

    static List<Process> readProcesses() {
        int n = readInt("Enter number of processes (1-20): ", 1, 20);
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            System.out.println("Process P" + (i + 1) + ":");
            int at = readInt("  Arrival time  (0-1000): ", 0, 1000);
            int bt = readInt("  Burst time    (1-1000): ", 1, 1000);
            int pr = readInt("  Priority      (1-100, smaller = higher): ", 1, 100);
            list.add(new Process(i, "P" + (i + 1), at, bt, pr));
        }
        return list;
    }

    static List<Process> copyAll(List<Process> original) {
        List<Process> copies = new ArrayList<>();
        for (Process p : original) copies.add(p.copy());
        return copies;
    }

    /** Adds a segment; joins it to the previous one if the same process continues. */
    static void addSegment(List<Segment> timeline, String pid, int start, int end) {
        if (!timeline.isEmpty()) {
            Segment last = timeline.get(timeline.size() - 1);
            if (last.pid.equals(pid) && last.end == start) {
                last.end = end;
                return;
            }
        }
        timeline.add(new Segment(pid, start, end));
    }

    // ------------------------------------------------------------------
    // Scheduling algorithms
    // ------------------------------------------------------------------

    /**
     * Generic non-preemptive scheduler. Whenever the CPU is free, the arrived
     * process that comes first according to 'rule' is run to completion.
     * FCFS, SJF and Priority differ only in this rule.
     */
    static Result nonPreemptive(String name, List<Process> original, Comparator<Process> rule) {
        List<Process> procs = copyAll(original);
        List<Process> notDone = new ArrayList<>(procs);
        List<Segment> timeline = new ArrayList<>();
        int time = 0;

        while (!notDone.isEmpty()) {
            Process chosen = null;
            for (Process p : notDone) {
                if (p.arrivalTime <= time && (chosen == null || rule.compare(p, chosen) < 0)) {
                    chosen = p;
                }
            }
            if (chosen == null) {
                // nobody has arrived yet: CPU is idle until the next arrival
                int next = Integer.MAX_VALUE;
                for (Process p : notDone) next = Math.min(next, p.arrivalTime);
                addSegment(timeline, "IDLE", time, next);
                time = next;
                continue;
            }
            addSegment(timeline, chosen.id, time, time + chosen.burstTime);
            time += chosen.burstTime;
            chosen.finish(time);
            notDone.remove(chosen);
        }
        return new Result(name, procs, timeline);
    }

    static Result fcfs(List<Process> original) {
        return nonPreemptive("FCFS", original, new Comparator<Process>() {
            public int compare(Process a, Process b) {
                if (a.arrivalTime != b.arrivalTime) return Integer.compare(a.arrivalTime, b.arrivalTime);
                return Integer.compare(a.index, b.index);
            }
        });
    }

    static Result sjf(List<Process> original) {
        return nonPreemptive("SJF (Non-Preemptive)", original, new Comparator<Process>() {
            public int compare(Process a, Process b) {
                if (a.burstTime != b.burstTime) return Integer.compare(a.burstTime, b.burstTime);
                if (a.arrivalTime != b.arrivalTime) return Integer.compare(a.arrivalTime, b.arrivalTime);
                return Integer.compare(a.index, b.index);
            }
        });
    }

    static Result priority(List<Process> original) {
        return nonPreemptive("Priority (Non-Preemptive)", original, new Comparator<Process>() {
            public int compare(Process a, Process b) {
                if (a.priority != b.priority) return Integer.compare(a.priority, b.priority);
                if (a.arrivalTime != b.arrivalTime) return Integer.compare(a.arrivalTime, b.arrivalTime);
                return Integer.compare(a.index, b.index);
            }
        });
    }

    static Result roundRobin(List<Process> original, int quantum) {
        List<Process> procs = copyAll(original);
        List<Process> byArrival = new ArrayList<>(procs);
        byArrival.sort(new Comparator<Process>() {
            public int compare(Process a, Process b) {
                if (a.arrivalTime != b.arrivalTime) return Integer.compare(a.arrivalTime, b.arrivalTime);
                return Integer.compare(a.index, b.index);
            }
        });

        Queue<Process> ready = new LinkedList<>();
        List<Segment> timeline = new ArrayList<>();
        int time = 0;
        int next = 0;       // next process in byArrival to be admitted
        int finished = 0;

        while (finished < procs.size()) {
            while (next < byArrival.size() && byArrival.get(next).arrivalTime <= time) {
                ready.add(byArrival.get(next++));
            }
            if (ready.isEmpty()) {
                int arrival = byArrival.get(next).arrivalTime;
                addSegment(timeline, "IDLE", time, arrival);
                time = arrival;
                continue;
            }
            Process current = ready.poll();
            int run = Math.min(quantum, current.remainingTime);
            addSegment(timeline, current.id, time, time + run);
            time += run;
            current.remainingTime -= run;

            // processes that arrived during this slice join the queue before the current one
            while (next < byArrival.size() && byArrival.get(next).arrivalTime <= time) {
                ready.add(byArrival.get(next++));
            }
            if (current.remainingTime == 0) {
                current.finish(time);
                finished++;
            } else {
                ready.add(current);
            }
        }
        return new Result("Round Robin (Quantum = " + quantum + ")", procs, timeline);
    }

    // ------------------------------------------------------------------
    // Output
    // ------------------------------------------------------------------

    static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    static void printGantt(List<Segment> timeline) {
        StringBuilder bars = new StringBuilder("|");
        StringBuilder times = new StringBuilder();
        times.append(timeline.get(0).start);
        int position = String.valueOf(timeline.get(0).start).length();

        for (Segment s : timeline) {
            String label = " " + s.pid + " ";
            bars.append(label).append("|");
            String end = String.valueOf(s.end);
            int target = bars.length();
            // place the end time under the closing bar
            int pad = target - position - end.length();
            times.append(repeat(' ', Math.max(pad, 1))).append(end);
            position = times.length();
        }
        System.out.println("Gantt chart:");
        System.out.println(bars);
        System.out.println(times);
    }

    static void printResult(Result r) {
        System.out.println();
        System.out.println("=== " + r.algorithmName + " ===");
        printGantt(r.timeline);
        System.out.println();
        System.out.printf("%-8s %-6s %-6s %-9s %-6s %-6s %-6s%n",
                "Process", "AT", "BT", "Priority", "CT", "TAT", "WT");
        System.out.println(repeat('-', 52));
        for (Process p : r.processes) {
            System.out.printf("%-8s %-6d %-6d %-9d %-6d %-6d %-6d%n",
                    p.id, p.arrivalTime, p.burstTime, p.priority,
                    p.completionTime, p.turnaroundTime, p.waitingTime);
        }
        System.out.println(repeat('-', 52));
        System.out.printf("Average Waiting Time    = %.2f%n", r.averageWaitingTime());
        System.out.printf("Average Turnaround Time = %.2f%n", r.averageTurnaroundTime());
    }

    static void printComparison(List<Result> results) {
        System.out.println();
        System.out.println("=== Comparison of all algorithms ===");
        System.out.printf("%-28s %-12s %-12s%n", "Algorithm", "Avg WT", "Avg TAT");
        System.out.println(repeat('-', 54));
        for (Result r : results) {
            System.out.printf("%-28s %-12.2f %-12.2f%n",
                    r.algorithmName, r.averageWaitingTime(), r.averageTurnaroundTime());
        }
    }

    // ------------------------------------------------------------------
    // Main menu
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=====================================");
        System.out.println("      CPU SCHEDULING SIMULATOR");
        System.out.println("=====================================");

        List<Process> processes = readProcesses();

        while (true) {
            System.out.println();
            System.out.println("------------- MENU -------------");
            System.out.println("1. First Come First Served (FCFS)");
            System.out.println("2. Shortest Job First (SJF, non-preemptive)");
            System.out.println("3. Priority Scheduling (non-preemptive)");
            System.out.println("4. Round Robin");
            System.out.println("5. Compare all algorithms");
            System.out.println("6. Enter new processes");
            System.out.println("0. Exit");
            int choice = readInt("Enter your choice: ", 0, 6);

            switch (choice) {
                case 1:
                    printResult(fcfs(processes));
                    break;
                case 2:
                    printResult(sjf(processes));
                    break;
                case 3:
                    printResult(priority(processes));
                    break;
                case 4: {
                    int q = readInt("Enter time quantum (1-100): ", 1, 100);
                    printResult(roundRobin(processes, q));
                    break;
                }
                case 5: {
                    int q = readInt("Enter time quantum for Round Robin (1-100): ", 1, 100);
                    List<Result> all = new ArrayList<>();
                    all.add(fcfs(processes));
                    all.add(sjf(processes));
                    all.add(priority(processes));
                    all.add(roundRobin(processes, q));
                    for (Result r : all) printResult(r);
                    printComparison(all);
                    break;
                }
                case 6:
                    processes = readProcesses();
                    break;
                case 0:
                    System.out.println("Thank you for using the CPU Scheduling Simulator.");
                    return;
                default:
                    break;
            }
        }
    }
}
