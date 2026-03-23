import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.*;

// ===== MAIN APP =====
public class AttendanceApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}

// ===== MODELS =====
abstract class User {
    String name;
    String username;
    String password;
    String email;

    User(String name, String username, String password, String email) {
        this.name = name; this.username = username; this.password = password; this.email = email;
    }
}

class Teacher extends User {
    Teacher(String name, String username, String password, String email) {
        super(name, username, password, email);
    }
}

class Student extends User {
    Map<String, Set<LocalDate>> attendance = new HashMap<>();
    Student(String name, String username, String password, String email) {
        super(name, username, password, email);
    }
    void mark(String subject, LocalDate date) {
        attendance.computeIfAbsent(subject, k -> new HashSet<>()).add(date);
    }
    int totalFor(String subject) {
        return attendance.getOrDefault(subject, Collections.emptySet()).size();
    }
}

class Subject {
    final String name;
    boolean attendanceOpen = true;
    Set<String> markedToday = new HashSet<>();
    Subject(String name) { this.name = name; }
    void resetDailyMarks() { markedToday.clear(); }
}

class DataStore {
    private static DataStore INSTANCE;
    Map<String, Teacher> teachers = new HashMap<>();
    Map<String, Student> students = new HashMap<>();
    Map<String, Subject> subjects = new HashMap<>();
    private DataStore() {}
    static DataStore get() { if (INSTANCE == null) INSTANCE = new DataStore(); return INSTANCE; }
    boolean hasAnyTeacher() { return !teachers.isEmpty(); }
}

// ===== APP FRAME =====
class AppFrame extends JFrame {
    CardLayout cards = new CardLayout();
    JPanel root = new JPanel(cards);
    MainMenuPanel mainMenuPanel;
    TeacherLoginPanel teacherLoginPanel;
    StudentLoginPanel studentLoginPanel;
    TeacherDashboardPanel teacherDashboardPanel;
    StudentDashboardPanel studentDashboardPanel;

    AppFrame() {
        super("Attendance System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        mainMenuPanel = new MainMenuPanel(this);
        teacherLoginPanel = new TeacherLoginPanel(this);
        studentLoginPanel = new StudentLoginPanel(this);
        teacherDashboardPanel = new TeacherDashboardPanel(this);
        studentDashboardPanel = new StudentDashboardPanel(this);

        root.add(mainMenuPanel, "main");
        root.add(teacherLoginPanel, "tlogin");
        root.add(studentLoginPanel, "slogin");
        root.add(teacherDashboardPanel, "tdash");
        root.add(studentDashboardPanel, "sdash");

        setContentPane(root);
        showMain();
    }

    void showMain() { cards.show(root, "main"); }
    void showTeacherLogin() { cards.show(root, "tlogin"); }
    void showStudentLogin() { cards.show(root, "slogin"); }
    void showTeacherDash(Teacher t) { teacherDashboardPanel.setTeacher(t); cards.show(root, "tdash"); }
    void showStudentDash(Student s) { studentDashboardPanel.setStudent(s); cards.show(root, "sdash"); }
}

// ===== MAIN MENU =====
class MainMenuPanel extends JPanel {
    MainMenuPanel(AppFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10,10,10,10);
        gc.gridx = 0; gc.gridy = 0;

        JLabel title = new JLabel("Welcome – Attendance Portal");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        add(title, gc);

        gc.gridy++;
        JButton teacherBtn = new JButton("Teacher Login / Domain");
        teacherBtn.addActionListener(e -> frame.showTeacherLogin());
        add(teacherBtn, gc);

        gc.gridy++;
        JButton studentBtn = new JButton("Student Login");
        studentBtn.addActionListener(e -> {
            if (!DataStore.get().hasAnyTeacher()) {
                JOptionPane.showMessageDialog(this,
                        "No teacher domain exists yet. Please create it first.",
                        "Access blocked", JOptionPane.WARNING_MESSAGE);
                frame.showTeacherLogin();
            } else {
                frame.showStudentLogin();
            }
        });
        add(studentBtn, gc);
    }
}

// ===== TEACHER LOGIN =====
class TeacherLoginPanel extends JPanel {
    JTextField user = new JTextField(20);
    JPasswordField pass = new JPasswordField(20);
    JTextField name = new JTextField(20);
    JTextField email = new JTextField(20);

    TeacherLoginPanel(AppFrame frame) {
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.gridx=0; gc.gridy=0; gc.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Username:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(user, gc);
        gc.gridx=0; gc.gridy++; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Password:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(pass, gc);
        gc.gridx=0; gc.gridy++; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Name:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(name, gc);
        gc.gridx=0; gc.gridy++; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Email:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(email, gc);

        JPanel buttons = new JPanel();
        JButton login = new JButton("Login");
        JButton signup = new JButton("Create Teacher Domain");
        JButton back = new JButton("Back");
        buttons.add(login); buttons.add(signup); buttons.add(back);

        login.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pass.getPassword());
            Teacher t = DataStore.get().teachers.get(u);
            if (t != null && Objects.equals(t.password, p)) {
                frame.showTeacherDash(t);
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        signup.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pass.getPassword());
            String n = name.getText().trim();
            String em = email.getText().trim();
            if (u.isEmpty() || p.isEmpty() || n.isEmpty() || em.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields."); return;
            }
            if (DataStore.get().teachers.containsKey(u)) {
                JOptionPane.showMessageDialog(this, "Username exists."); return;
            }
            Teacher t = new Teacher(n,u,p,em);
            DataStore.get().teachers.put(u,t);
            JOptionPane.showMessageDialog(this,"Teacher domain created.");
            clearFields();
        });

        back.addActionListener(e -> { frame.showMain(); clearFields(); });

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }
    void clearFields() { user.setText(""); pass.setText(""); name.setText(""); email.setText(""); }
}

// ===== STUDENT LOGIN =====
class StudentLoginPanel extends JPanel {
    JTextField user = new JTextField(20);
    JPasswordField pass = new JPasswordField(20);

    StudentLoginPanel(AppFrame frame) {
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.gridx=0; gc.gridy=0; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Username:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(user, gc);
        gc.gridx=0; gc.gridy++; gc.anchor = GridBagConstraints.EAST; form.add(new JLabel("Password:"), gc);
        gc.gridx=1; gc.anchor = GridBagConstraints.WEST; form.add(pass, gc);

        JPanel buttons = new JPanel();
        JButton login = new JButton("Login");
        JButton back = new JButton("Back");
        buttons.add(login); buttons.add(back);

        login.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pass.getPassword());
            Student s = DataStore.get().students.get(u);
            if (s != null && Objects.equals(s.password, p)) {
                frame.showStudentDash(s);
                user.setText(""); pass.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        back.addActionListener(e -> frame.showMain());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }
}

// ===== TEACHER DASHBOARD =====
class TeacherDashboardPanel extends JPanel {
    Teacher current;
    JLabel hello = new JLabel();
    TeacherDashboardPanel(AppFrame frame) {
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        hello.setFont(hello.getFont().deriveFont(Font.BOLD, 18f));
        top.add(hello, BorderLayout.WEST);
        JButton back = new JButton("Logout");
        back.addActionListener(e -> frame.showMain());
        top.add(back, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(0,1,8,8));
        JButton checkAtt = new JButton("1) Check & Control Attendance");
        JButton addStudent = new JButton("2) Add Student");
        JButton addSubject = new JButton("3) Add Subjects");
        actions.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        actions.add(checkAtt); actions.add(addStudent); actions.add(addSubject);
        add(actions, BorderLayout.CENTER);

        checkAtt.addActionListener(e -> new TeacherAttendanceDialog(frame, current).setVisible(true));
        addStudent.addActionListener(e -> new AddStudentDialog(frame).setVisible(true));
        addSubject.addActionListener(e -> new AddSubjectDialog(frame).setVisible(true));
    }
    void setTeacher(Teacher t){ this.current=t; hello.setText("Teacher: "+t.name); }
}

// ===== STUDENT DASHBOARD =====
class StudentDashboardPanel extends JPanel {
    Student current;
    JLabel hello=new JLabel();
    StudentDashboardPanel(AppFrame frame){
        setLayout(new BorderLayout());
        JPanel top=new JPanel(new BorderLayout());
        hello.setFont(hello.getFont().deriveFont(Font.BOLD,18f));
        top.add(hello,BorderLayout.WEST);
        JButton back=new JButton("Logout");
        back.addActionListener(e->frame.showMain());
        top.add(back,BorderLayout.EAST);
        add(top,BorderLayout.NORTH);
        JPanel actions=new JPanel(new GridLayout(0,1,8,8));
        JButton mark=new JButton("1) Mark Attendance");
        JButton check=new JButton("2) Check Attendance");
        actions.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        actions.add(mark);actions.add(check);
        add(actions,BorderLayout.CENTER);
        mark.addActionListener(e->new StudentMarkDialog((JFrame)SwingUtilities.getWindowAncestor(this),current).setVisible(true));
        check.addActionListener(e->new StudentCheckDialog((JFrame)SwingUtilities.getWindowAncestor(this),current).setVisible(true));
    }
    void setStudent(Student s){ this.current=s; hello.setText("Student: "+s.name); }
}
// ===== ADD SUBJECT DIALOG =====
class AddSubjectDialog extends JDialog {
    JTextField subjectName = new JTextField(20);
    DefaultListModel<String> listModel = new DefaultListModel<>();
    AddSubjectDialog(JFrame owner) {
        super(owner, "Add Subjects", true);
        setSize(450, 350);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        DataStore ds = DataStore.get();
        ds.subjects.keySet().forEach(listModel::addElement);
        JList<String> list = new JList<>(listModel);
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        form.add(new JLabel("Subject name:"), BorderLayout.WEST);
        form.add(subjectName, BorderLayout.CENTER);
        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> {
            String name = subjectName.getText().trim();
            if (name.isEmpty()) return;
            if (ds.subjects.containsKey(name)) {
                JOptionPane.showMessageDialog(this, "Subject exists.");
                return;
            }
            ds.subjects.put(name, new Subject(name));
            listModel.addElement(name);
            subjectName.setText("");
        });
        form.add(addBtn, BorderLayout.EAST);
        add(form, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        JButton close = new JButton("Done");
        close.addActionListener(e -> dispose());
        add(close, BorderLayout.SOUTH);
    }
}

// ===== ADD STUDENT DIALOG =====
class AddStudentDialog extends JDialog {
    JTextField name = new JTextField(18);
    JTextField user = new JTextField(14);
    JPasswordField pass = new JPasswordField(14);
    JTextField email = new JTextField(18);
    AddStudentDialog(JFrame owner) {
        super(owner, "Add Student", true);
        setSize(480, 260);
        setLocationRelativeTo(owner);
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.gridx = 0; gc.gridy = 0; add(new JLabel("Name:"), gc);
        gc.gridx = 1; add(name, gc);
        gc.gridx = 0; gc.gridy++; add(new JLabel("Username:"), gc);
        gc.gridx = 1; add(user, gc);
        gc.gridx = 0; gc.gridy++; add(new JLabel("Password:"), gc);
        gc.gridx = 1; add(pass, gc);
        gc.gridx = 0; gc.gridy++; add(new JLabel("Email:"), gc);
        gc.gridx = 1; add(email, gc);
        gc.gridx = 0; gc.gridy++; gc.gridwidth = 2;
        JButton addBtn = new JButton("Create Student");
        add(addBtn, gc);
        addBtn.addActionListener(e -> {
            String n = name.getText().trim();
            String u = user.getText().trim();
            String p = new String(pass.getPassword());
            String em = email.getText().trim();
            if (n.isEmpty() || u.isEmpty() || p.isEmpty() || em.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.");
                return;
            }
            DataStore ds = DataStore.get();
            if (ds.students.containsKey(u)) {
                JOptionPane.showMessageDialog(this, "Username exists.");
                return;
            }
            ds.students.put(u, new Student(n, u, p, em));
            JOptionPane.showMessageDialog(this, "Student created: " + n);
            // open webcam for face registration
            FaceCapture.registerFace(u);
            dispose();
        });
    }
}

// ===== TEACHER ATTENDANCE CONTROL =====
class TeacherAttendanceDialog extends JDialog {
    DefaultListModel<String> subjectsModel = new DefaultListModel<>();
    JLabel statusLabel = new JLabel("Select a subject to manage");
    JLabel countsLabel = new JLabel("Students added: 0");

    TeacherAttendanceDialog(JFrame owner, Teacher t) {
        super(owner, "Attendance Control", true);
        setSize(700, 470);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        DataStore ds = DataStore.get();
        ds.subjects.keySet().forEach(subjectsModel::addElement);
        JList<String> list = new JList<>(subjectsModel);
        add(new JScrollPane(list), BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JButton toggle = new JButton("Open / Close Attendance");
        JButton todayBtn = new JButton("View Today's Attendance");
        JButton email = new JButton("Send Attendance via Email");
        JButton report = new JButton("Show Student Percentages");

        right.add(Box.createVerticalStrut(10));
        right.add(statusLabel);
        right.add(Box.createVerticalStrut(10));
        right.add(toggle);
        right.add(Box.createVerticalStrut(8));
        right.add(todayBtn);
        right.add(Box.createVerticalStrut(8));
        right.add(email);
        right.add(Box.createVerticalStrut(8));
        right.add(report);
        right.add(Box.createVerticalGlue());

        add(right, BorderLayout.CENTER);
        add(countsLabel, BorderLayout.SOUTH);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String s = list.getSelectedValue();
                if (s != null) {
                    Subject sub = ds.subjects.get(s);
                    statusLabel.setText("Subject: " + s + " | Attendance is " +
                            (sub.attendanceOpen ? "OPEN" : "CLOSED"));
                    countsLabel.setText("Students added: " + ds.students.size());
                }
            }
        });

        toggle.addActionListener(e -> {
            String s = list.getSelectedValue();
            if (s == null) return;
            Subject sub = ds.subjects.get(s);
            sub.attendanceOpen = !sub.attendanceOpen;
            statusLabel.setText("Subject: " + s + " | Attendance is " +
                    (sub.attendanceOpen ? "OPEN" : "CLOSED"));
            if (!sub.attendanceOpen) {
                sub.resetDailyMarks();
                JOptionPane.showMessageDialog(this,
                        "Attendance for " + s + " has been closed.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Attendance for " + s + " is now OPEN.");
            }
        });

        todayBtn.addActionListener(e -> {
            String s = list.getSelectedValue();
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Select a subject first.");
                return;
            }
            new TeacherTodayDialog((JFrame) owner, s).setVisible(true);
        });

        email.addActionListener(e -> {
    String s = list.getSelectedValue();
       if (s == null) {
         JOptionPane.showMessageDialog(this, "Select a subject first.");
         return;
       }
        new EmailOneStudentDialog((JFrame) owner, t, s).setVisible(true);
        });

        report.addActionListener(e -> new TeacherReportDialog((JFrame) owner).setVisible(true));
    }
}

// ===== TEACHER TODAY DIALOG =====
class TeacherTodayDialog extends JDialog {
    TeacherTodayDialog(JFrame owner, String subject) {
        super(owner, "Today's Attendance – " + subject, true);
        setSize(520, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        DefaultListModel<String> model = new DefaultListModel<>();
        Subject sub = DataStore.get().subjects.get(subject);
        LocalDate today = LocalDate.now();
        for (Student st : DataStore.get().students.values()) {
            boolean present = st.attendance.getOrDefault(subject, Collections.emptySet()).contains(today);
            if (present) model.addElement(st.name + " (" + st.username + ")");
        }
        JList<String> list = new JList<>(model);
        add(new JScrollPane(list), BorderLayout.CENTER);
        JLabel info = new JLabel("Present today: " + model.size());
        info.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(info, BorderLayout.SOUTH);
    }
}

// ===== TEACHER REPORT =====
class TeacherReportDialog extends JDialog {
    TeacherReportDialog(JFrame owner) {
        super(owner, "Attendance Percentages", true);
        setSize(760, 420);
        setLocationRelativeTo(owner);
        String[] cols = {"Student", "Username", "Subject", "Classes Held", "Present", "Percent %"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        DataStore ds = DataStore.get();
        Map<String, Set<LocalDate>> subjectDates = new HashMap<>();
        for (Student s : ds.students.values()) {
            for (Map.Entry<String, Set<LocalDate>> e : s.attendance.entrySet()) {
                subjectDates.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue());
            }
        }
        for (Student s : ds.students.values()) {
            for (String subj : ds.subjects.keySet()) {
                int total = subjectDates.getOrDefault(subj, Collections.emptySet()).size();
                int present = s.totalFor(subj);
                double pct = total == 0 ? 0 : (present * 100.0 / total);
                model.addRow(new Object[]{s.name, s.username, subj, total, present,
                        String.format(java.util.Locale.US, "%.1f", pct)});
            }
        }
        JTable table = new JTable(model);
        add(new JScrollPane(table));
    }
}

// ===== STUDENT MARK ATTENDANCE =====
class StudentMarkDialog extends JDialog {
    DefaultListModel<String> subjectsModel = new DefaultListModel<>();
    StudentMarkDialog(JFrame owner, Student s) {
        super(owner, "Mark Attendance", true);
        setSize(520, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        DataStore ds = DataStore.get();
        ds.subjects.keySet().forEach(subjectsModel::addElement);
        JList<String> list = new JList<>(subjectsModel);
        add(new JScrollPane(list), BorderLayout.CENTER);
        JButton markBtn = new JButton("Capture Face & Mark");
        add(markBtn, BorderLayout.SOUTH);
        markBtn.addActionListener((ActionEvent e) -> {
            String subject = list.getSelectedValue();
            if (subject == null) { JOptionPane.showMessageDialog(this, "Select a subject."); return; }
            Subject sub = ds.subjects.get(subject);
            if (!sub.attendanceOpen) { JOptionPane.showMessageDialog(this, "Attendance closed."); return; }
            boolean ok = FaceCapture.verifyFace(s.username);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Face not matched! Marked ABSENT.", "Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            LocalDate today = LocalDate.now();
            if (sub.markedToday.contains(s.username)) {
                JOptionPane.showMessageDialog(this, "Already marked."); return;
            }
            s.mark(subject, today); sub.markedToday.add(s.username);
            JOptionPane.showMessageDialog(this, "Marked PRESENT for '" + subject + "' on " + today + ".");
        });
    }
}

// ===== STUDENT CHECK ATTENDANCE =====
class StudentCheckDialog extends JDialog {
    StudentCheckDialog(JFrame owner, Student s) {
        super(owner, "Your Attendance", true);
        setSize(640, 400);
        setLocationRelativeTo(owner);
        String[] cols = {"Subject", "Dates Present", "Classes Held", "Presents", "Percent %"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        DataStore ds = DataStore.get();
        Map<String, Set<LocalDate>> subjectDates = new HashMap<>();
        for (Student other : ds.students.values()) {
            for (Map.Entry<String, Set<LocalDate>> e : other.attendance.entrySet()) {
                subjectDates.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue());
            }
        }
        for (String subj : ds.subjects.keySet()) {
            Set<LocalDate> myDates = s.attendance.getOrDefault(subj, Collections.emptySet());
            int total = subjectDates.getOrDefault(subj, Collections.emptySet()).size();
            int present = myDates.size();
            double pct = total == 0 ? 0 : (present * 100.0 / total);
            model.addRow(new Object[]{subj, fmtDates(myDates), total, present,
                    String.format(java.util.Locale.US, "%.1f", pct)});
        }
        JTable table = new JTable(model);
        add(new JScrollPane(table));
    }
    private String fmtDates(Set<LocalDate> dates) {
        if (dates.isEmpty()) return "-";
        java.util.List<LocalDate> list = new java.util.ArrayList<>(dates);
        list.sort(java.util.Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder();
        for (LocalDate d : list) sb.append(d).append(", ");
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }
}

// ===== EMAIL ONE STUDENT DIALOG =====
class EmailOneStudentDialog extends JDialog {
    private final Teacher teacher;
    private final String subjectName;
    private final JList<String> studentList;

    EmailOneStudentDialog(JFrame owner, Teacher teacher, String subjectName) {
        super(owner, "Send Attendance to One Student", true);
        this.teacher = teacher;
        this.subjectName = subjectName;

        setSize(420, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        DefaultListModel<String> model = new DefaultListModel<>();
        DataStore ds = DataStore.get();
        for (Student s : ds.students.values()) {
            model.addElement(s.name + " (" + s.username + ")  <" + s.email + ">");
        }
        studentList = new JList<>(model);

        add(new JLabel("Select a student to email for subject: " + subjectName),
                BorderLayout.NORTH);
        add(new JScrollPane(studentList), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton send = new JButton("Send Email");
        JButton cancel = new JButton("Cancel");
        bottom.add(send);
        bottom.add(cancel);
        add(bottom, BorderLayout.SOUTH);

        send.addActionListener(e -> onSend());
        cancel.addActionListener(e -> dispose());
    }

    private void onSend() {
        String selected = studentList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a student.");
            return;
        }
        int open = selected.indexOf('(');
        int close = selected.indexOf(')');
        if (open < 0 || close < 0 || close <= open + 1) {
            JOptionPane.showMessageDialog(this, "Could not parse selected student.");
            return;
        }
        String username = selected.substring(open + 1, close).trim();

        DataStore ds = DataStore.get();
        Student st = ds.students.get(username);
        if (st == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            return;
        }

        LocalDate today = LocalDate.now();
        String log = Mailer.sendAttendanceToStudentStub(
                teacher.email,
                st,
                subjectName,
                today
        );

        JOptionPane.showMessageDialog(this,
                new JScrollPane(new JTextArea(log, 12, 30)),
                "Email Log (simulation)",
                JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}

// ===== MAILER (REAL SMTP USING JAKARTA MAIL) =====
class Mailer {

    // Core sender using Gmail SMTP (TLS on 587)
    private static void sendEmail(String from, String to, String subject, String body) {
        String host = System.getenv("SMTP_HOST");   // e.g. smtp.gmail.com
        String port = System.getenv("SMTP_PORT");   // e.g. 587
        String user = System.getenv("SMTP_USER");   // your Gmail address
        String pass = System.getenv("SMTP_PASS");   // 16-char App Password

        if (host == null || port == null || user == null || pass == null) {
            throw new RuntimeException(
                "SMTP env vars missing. Set SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS before running.");
        }

        // Gmail is strict: From should usually equal SMTP_USER. To be safe, force it.
        // If you want to use the teacher's email as the visual "from", set it as Reply-To instead.
        String enforcedFrom = user;

        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");  // TLS (port 587)
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        // props.put("mail.debug", "true"); // uncomment for verbose logs

        jakarta.mail.Session session = jakarta.mail.Session.getInstance(props,
            new jakarta.mail.Authenticator() {
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(user, pass);
                }
            });

        try {
            jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
            msg.setFrom(new jakarta.mail.internet.InternetAddress(enforcedFrom));
            msg.setRecipients(jakarta.mail.Message.RecipientType.TO,
                    jakarta.mail.internet.InternetAddress.parse(to, false));
            msg.setSubject(subject, java.nio.charset.StandardCharsets.UTF_8.name());
            msg.setText(body, java.nio.charset.StandardCharsets.UTF_8.name());

            // Optional: preserve teacher email as Reply-To if different
            if (from != null && !from.isEmpty() && !from.equalsIgnoreCase(enforcedFrom)) {
                msg.setReplyTo(new jakarta.mail.Address[]{
                    new jakarta.mail.internet.InternetAddress(from)
                });
            }

            jakarta.mail.Transport.send(msg);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    // Send to ALL students for a subject on a given date (bulk)
    static String sendAttendanceStub(String subject, java.time.LocalDate date) {
        String teacherFrom = findAnyTeacherEmailOrFallback();
        StringBuilder log = new StringBuilder();
        log.append("Sending attendance for '").append(subject).append("' on ").append(date).append(":\n\n");

        for (Student st : DataStore.get().students.values()) {
            boolean present = st.attendance
                    .getOrDefault(subject, java.util.Collections.emptySet())
                    .contains(date);

            String emailBody =
                "Dear " + st.name + ",\n\n" +
                "Your attendance status for " + subject + " on " + date + " is: " +
                (present ? "PRESENT" : "ABSENT") + ".\n\n" +
                "-- Sent by Attendance System\n";

            try {
                sendEmail(teacherFrom, st.email,
                        "Attendance for '" + subject + "' on " + date, emailBody);
                log.append(st.email).append(" -> SENT (")
                   .append(present ? "PRESENT" : "ABSENT").append(")\n");
            } catch (RuntimeException ex) {
                log.append(st.email).append(" -> FAILED: ").append(ex.getMessage()).append("\n");
            }
        }
        return log.toString();
    }

    // Send to ONE selected student (used by EmailOneStudentDialog)
    static String sendAttendanceToStudentStub(String fromTeacherEmail, Student st, String subject, java.time.LocalDate date) {
        boolean present = st.attendance
                .getOrDefault(subject, java.util.Collections.emptySet())
                .contains(date);

        String emailBody =
            "Dear " + st.name + ",\n\n" +
            "Your attendance status for " + subject + " on " + date + " is: " +
            (present ? "PRESENT" : "ABSENT") + ".\n\n" +
            "-- Sent by Attendance System\n";

        StringBuilder log = new StringBuilder();
        try {
            sendEmail(fromTeacherEmail, st.email,
                    "Attendance for '" + subject + "' on " + date, emailBody);
            log.append("From: ").append(fromTeacherEmail).append("\n");
            log.append("To: ").append(st.email).append("\n");
            log.append("Result: SENT\n");
        } catch (RuntimeException ex) {
            log.append("From: ").append(fromTeacherEmail).append("\n");
            log.append("To: ").append(st.email).append("\n");
            log.append("Result: FAILED -> ").append(ex.getMessage()).append("\n");
        }
        return log.toString();
    }

    // Helper: default From for bulk mode
    private static String findAnyTeacherEmailOrFallback() {
        for (Teacher t : DataStore.get().teachers.values()) {
            if (t.email != null && !t.email.isEmpty()) return t.email;
        }
        String user = System.getenv("SMTP_USER");
        return (user != null && !user.isEmpty()) ? user : "no-reply@example.com";
    }
}