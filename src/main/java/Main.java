import java.util.concurrent.*;
import java.util.*;

class ElevatorSystem {
    private final int numFloors;
    private final List<Elevator> elevators;
    private final BlockingQueue<CallRequest> requests = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public ElevatorSystem(int numElevators, int numFloors) {
        this.numFloors = numFloors;
        this.elevators = new ArrayList<>();
        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(i + 1, numFloors));
        }
    }

    public void start() {
        for (Elevator elevator : elevators) {
            executor.submit(() -> {
                while (true) {
                    elevator.processRequest(requests.poll(500, TimeUnit.MILLISECONDS));
                }
            });
        }
        new Thread(this::generateRequests).start();
    }

    private void generateRequests() {
        Random random = new Random();
        while (true) {
            try {
                Thread.sleep(random.nextInt(3000) + 1000);
                int fromFloor = random.nextInt(numFloors) + 1;
                int toFloor = random.nextInt(numFloors) + 1;
                while (fromFloor == toFloor) {
                    toFloor = random.nextInt(numFloors) + 1;
                }
                CallRequest request = new CallRequest(fromFloor, toFloor);
                requests.put(request);
                System.out.println("Вызов лифта: с " + fromFloor + " этажа на " + toFloor + " этаж.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static class Elevator {
        private final int id;
        private final int numFloors;
        private int currentFloor = 1;
        private final Queue<CallRequest> tasks = new PriorityQueue<>(Comparator.comparingInt(cr -> Math.abs(cr.from - currentFloor)));

        public Elevator(int id, int numFloors) {
            this.id = id;
            this.numFloors = numFloors;
        }

        public synchronized void processRequest(CallRequest request) {
            if (request != null) {
                tasks.offer(request);
            }
            if (!tasks.isEmpty()) {
                CallRequest nextTask = tasks.poll();
                moveToFloor(nextTask.from);
                System.out.println("Лифт " + id + " забрал пассажира с " + nextTask.from + " этажа.");
                moveToFloor(nextTask.to);
                System.out.println("Лифт " + id + " доставил пассажира на " + nextTask.to + " этаж.");
            }
        }

        private void moveToFloor(int targetFloor) {
            while (currentFloor != targetFloor) {
                if (currentFloor < targetFloor) {
                    currentFloor++;
                } else {
                    currentFloor--;
                }
                System.out.println("Лифт " + id + " на " + currentFloor + " этаже.");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static class CallRequest {
        private final int from;
        private final int to;

        public CallRequest(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    public static void main(String[] args) {
        ElevatorSystem system = new ElevatorSystem(3, 10);
        Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));
        system.start();
    }
}
