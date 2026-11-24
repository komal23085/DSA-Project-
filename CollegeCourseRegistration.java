import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

public class CollegeCourseRegistration {

    private static final String DATA_FILE = "registrations.txt";
    private static final int DEFAULT_CAPACITY = 2;

    // Singly Linked List for enrolled students
    static class StudentLinkedList {
        static class Node {
            String studentName;
            Node next;

            Node(String studentName) {
                this.studentName = studentName;
                this.next = null;
            }
        }

        private Node head;
        private Node tail;
        private int size;

        public void addLast(String studentName) {
            Node newNode = new Node(studentName);
            if (head == null) {
                head = tail = newNode;
            } else {
                tail.next = newNode;
                tail = newNode;
            }
            size++;
        }

        public boolean contains(String studentName) {
            Node curr = head;
            while (curr != null) {
                if (curr.studentName.equalsIgnoreCase(studentName)) {
                    return true;
                }
                curr = curr.next;
            }
            return false;
        }

        public int getSize() {
            return size;
        }

        public List<String> toList() {
            List<String> list = new ArrayList<>();
            Node curr = head;
            while (curr != null) {
                list.add(curr.studentName);
                curr = curr.next;
            }
            return list;
        }
    }

    // FIFO Queue for waitlisted students
    static class WaitlistQueue {
        private final Queue<String> queue = new LinkedList<>();

        public void enqueue(String studentName) {
            queue.offer(studentName);
        }

        public boolean contains(String studentName) {
            for (String s : queue) {
                if (s.equalsIgnoreCase(studentName)) {
                    return true;
                }
            }
            return false;
        }

        public List<String> toList() {
            return new ArrayList<>(queue);
        }
    }

    // Directed Graph for course prerequisite validation
    // Edge u -> v means course 'u' requires prerequisite 'v'
    static class PrerequisiteGraph {
        private final Map<String, List<String>> adjList = new HashMap<>();

        public void addCourse(String courseId) {
            adjList.putIfAbsent(courseId.toUpperCase(), new ArrayList<>());
        }

        public void addPrerequisite(String courseId, String prereqId) {
            String c = courseId.toUpperCase();
            String p = prereqId.toUpperCase();
            adjList.putIfAbsent(c, new ArrayList<>());
            adjList.putIfAbsent(p, new ArrayList<>());
            if (!adjList.get(c).contains(p)) {
                adjList.get(c).add(p);
            }
        }

        public boolean hasMetPrerequisites(String courseId, Set<String> completedCourses) {
            List<String> prereqs = adjList.getOrDefault(courseId.toUpperCase(), new ArrayList<>());
            for (String prereq : prereqs) {
                if (!completedCourses.contains(prereq.toUpperCase())) {
                    return false;
                }
            }
            return true;
        }

        public List<String> getPrerequisites(String courseId) {
            return adjList.getOrDefault(courseId.toUpperCase(), new ArrayList<>());
        }
    }

    static class Course {
        private final String courseId;
        private final String courseName;
        private final int capacity;
        private final StudentLinkedList enrolledStudents = new StudentLinkedList();
        private final WaitlistQueue waitlist = new WaitlistQueue();

        public Course(String courseId, String courseName, int capacity) {
            this.courseId = courseId.toUpperCase();
            this.courseName = courseName;
            this.capacity = capacity;
        }

        public String getCourseId() {
            return courseId;
        }

        public String getCourseName() {
            return courseName;
        }

        public int getCapacity() {
            return capacity;
        }

        public boolean isFull() {
            return enrolledStudents.getSize() >= capacity;
        }

        public boolean isEnrolled(String studentName) {
            return enrolledStudents.contains(studentName);
        }

        public boolean isWaitlisted(String studentName) {
            return waitlist.contains(studentName);
        }

        public void enrollStudent(String studentName) {
            enrolledStudents.addLast(studentName);
        }

        public void addToWaitlist(String studentName) {
            waitlist.enqueue(studentName);
        }

        public List<String> getEnrolledList() {
            return enrolledStudents.toList();
        }

        public List<String> getWaitlist() {
            return waitlist.toList();
        }
    }

    private final Map<String, Course> courseMap = new HashMap<>();
    private final PrerequisiteGraph prereqGraph = new PrerequisiteGraph();
    private final Map<String, Set<String>> studentCompletedCourses = new HashMap<>();

    public CollegeCourseRegistration() {
        initializeDefaultCatalog();
        loadFromFile();
    }

    private void initializeDefaultCatalog() {
        addCourseRecord("CS101", "Data Structures and Algorithms", DEFAULT_CAPACITY);
        addCourseRecord("CS102", "Object Oriented Programming", DEFAULT_CAPACITY);
        addCourseRecord("CS201", "Database Management Systems", DEFAULT_CAPACITY);
        addCourseRecord("CS202", "Operating Systems", DEFAULT_CAPACITY);

        // Example prerequisite edges: CS201 requires CS101; CS202 requires CS102
        prereqGraph.addPrerequisite("CS201", "CS101");
        prereqGraph.addPrerequisite("CS202", "CS102");
    }

    private void addCourseRecord(String id, String name, int capacity) {
        String key = id.toUpperCase();
        courseMap.putIfAbsent(key, new Course(key, name, capacity));
        prereqGraph.addCourse(key);
    }

    public void markCompleted(String studentName, String courseId) {
        String student = studentName.trim();
        String cId = courseId.trim().toUpperCase();
        if (student.isEmpty() || !courseMap.containsKey(cId)) {
            System.out.println("Invalid student name or unknown course " + cId + ".");
            return;
        }
        studentCompletedCourses
                .computeIfAbsent(student.toLowerCase(), k -> new HashSet<>())
                .add(cId);
        System.out.println("Recorded completed course " + cId + " for " + student + ".");
    }

    public String registerStudent(String studentName, String rawCourseId) {
        String student = studentName.trim();
        String courseId = rawCourseId.trim().toUpperCase();

        if (student.isEmpty()) {
            return "Student name cannot be empty.";
        }

        Course course = courseMap.get(courseId);
        if (course == null) {
            return "Course " + courseId + " not found.";
        }

        if (course.isEnrolled(student)) {
            return "Duplicate registration denied: " + student + " is already enrolled in " + courseId + ".";
        }

        if (course.isWaitlisted(student)) {
            return "Duplicate request denied: " + student + " is already in the waiting queue for " + courseId + ".";
        }

        Set<String> completed = studentCompletedCourses.getOrDefault(student.toLowerCase(), new HashSet<>());
        if (!prereqGraph.hasMetPrerequisites(courseId, completed)) {
            return "Prerequisites not met for " + student + " to register for " + courseId
                    + " (Required: " + prereqGraph.getPrerequisites(courseId) + ").";
        }

        if (!course.isFull()) {
            course.enrollStudent(student);
            return "Registration successful: " + student + " -> " + courseId;
        } else {
            course.addToWaitlist(student);
            return "Course full. " + student + " added to waiting queue.";
        }
    }

    public void displayAllCourses() {
        System.out.println("\n--- Course Enrolment & Waitlist Summary ---");
        for (Course c : courseMap.values()) {
            System.out.printf("%s (%s) | Capacity: %d | Enrolled: %s | Waitlist: %s%n",
                    c.getCourseId(),
                    c.getCourseName(),
                    c.getCapacity(),
                    c.getEnrolledList(),
                    c.getWaitlist());
        }
    }

    public void loadFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts[0].equals("COURSE") && parts.length >= 4) {
                    String cId = parts[1].trim().toUpperCase();
                    String cName = parts[2].trim();
                    int cap = Integer.parseInt(parts[3].trim());
                    addCourseRecord(cId, cName, cap);
                } else if (parts[0].equals("PREREQ") && parts.length >= 3) {
                    prereqGraph.addPrerequisite(parts[1].trim(), parts[2].trim());
                } else if (parts[0].equals("COMPLETED") && parts.length >= 3) {
                    String student = parts[1].trim();
                    String cId = parts[2].trim().toUpperCase();
                    studentCompletedCourses
                            .computeIfAbsent(student.toLowerCase(), k -> new HashSet<>())
                            .add(cId);
                } else if (parts[0].equals("ENROLLED") && parts.length >= 3) {
                    Course c = courseMap.get(parts[1].trim().toUpperCase());
                    if (c != null && !c.isFull()) {
                        c.enrollStudent(parts[2].trim());
                    }
                } else if (parts[0].equals("WAITLIST") && parts.length >= 3) {
                    Course c = courseMap.get(parts[1].trim().toUpperCase());
                    if (c != null) {
                        c.addToWaitlist(parts[2].trim());
                    }
                }
            }
            System.out.println("[INFO] Loaded courses and registrations from " + DATA_FILE);
        } catch (IOException | NumberFormatException e) {
            System.out.println("[WARN] Could not read " + DATA_FILE + ": " + e.getMessage());
        }
    }

    public void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (Course c : courseMap.values()) {
                writer.write("COURSE|" + c.getCourseId() + "|" + c.getCourseName() + "|" + c.getCapacity());
                writer.newLine();
            }
            for (Course c : courseMap.values()) {
                for (String prereq : prereqGraph.getPrerequisites(c.getCourseId())) {
                    writer.write("PREREQ|" + c.getCourseId() + "|" + prereq);
                    writer.newLine();
                }
            }
            for (Map.Entry<String, Set<String>> entry : studentCompletedCourses.entrySet()) {
                for (String doneCourse : entry.getValue()) {
                    writer.write("COMPLETED|" + entry.getKey() + "|" + doneCourse);
                    writer.newLine();
                }
            }
            for (Course c : courseMap.values()) {
                for (String student : c.getEnrolledList()) {
                    writer.write("ENROLLED|" + c.getCourseId() + "|" + student);
                    writer.newLine();
                }
                for (String student : c.getWaitlist()) {
                    writer.write("WAITLIST|" + c.getCourseId() + "|" + student);
                    writer.newLine();
                }
            }
            System.out.println("[INFO] Data saved to " + DATA_FILE);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to save " + DATA_FILE + ": " + e.getMessage());
        }
    }

    public void runStandardTestCases() {
        System.out.println("\n--- Running Standard Problem Statement Test Cases ---");
        // Reset CS101 for clean test demonstration
        courseMap.put("CS101", new Course("CS101", "Data Structures and Algorithms", DEFAULT_CAPACITY));

        // Test Case 1: Normal Input
        System.out.println("Test Case 1 (Normal): " + registerStudent("Aman", "CS101"));

        // Test Case 2: Boundary / Minimum Input (course full -> waiting queue)
        registerStudent("Neha", "CS101");
        System.out.println("Test Case 2 (Boundary/Full): " + registerStudent("Riya", "CS101"));

        // Test Case 3: Invalid Input (unknown course)
        System.out.println("Test Case 3 (Invalid Course): " + registerStudent("Kabir", "C999"));

        // Test Case 4: Different Valid Scenario (1 seat free -> Aman registers & seats updated)
        courseMap.put("CS101", new Course("CS101", "Data Structures and Algorithms", DEFAULT_CAPACITY));
        registerStudent("Neha", "CS101");
        String res4 = registerStudent("Aman", "CS101");
        int remainingSeats = courseMap.get("CS101").getCapacity() - courseMap.get("CS101").getEnrolledList().size();
        System.out.println("Test Case 4 (Valid Scenario): " + res4 + " | Available seats updated (remaining: " + remainingSeats + ")");
    }

    public static void main(String[] args) {
        CollegeCourseRegistration system = new CollegeCourseRegistration();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n==========================================");
            System.out.println("  COLLEGE COURSE REGISTRATION SYSTEM (PS 57)");
            System.out.println("==========================================");
            System.out.println("1. Register Student for a Course");
            System.out.println("2. Mark Prerequisite Course Completed");
            System.out.println("3. View Courses, Enrolled List & Waitlist");
            System.out.println("4. Run 4 Standard Test Cases");
            System.out.println("5. Save & Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.print("Enter Student Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter Course ID (e.g., CS101): ");
                    String cId = scanner.nextLine();
                    System.out.println(system.registerStudent(name, cId));
                    break;
                case "2":
                    System.out.print("Enter Student Name: ");
                    String sName = scanner.nextLine();
                    System.out.print("Enter Completed Course ID: ");
                    String doneId = scanner.nextLine();
                    system.markCompleted(sName, doneId);
                    break;
                case "3":
                    system.displayAllCourses();
                    break;
                case "4":
                    system.runStandardTestCases();
                    break;
                case "5":
                    system.saveToFile();
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please select 1 to 5.");
            }
        }
    }
}