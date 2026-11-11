package services;

import exceptions.DuplicateRecordException;
import exceptions.StudentNotFoundException;
import models.Student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owns the registered students: an ArrayList for ordered listing and a HashMap for ID lookup. */
public class StudentService {

    private final List<Student> students = new ArrayList<>();
    private final Map<Integer, Student> studentMap = new HashMap<>();

    public void registerStudent(Student student) throws DuplicateRecordException {
        if (studentMap.containsKey(student.getUserId())) {
            throw new DuplicateRecordException("A student with ID " + student.getUserId()
                    + " is already registered (" + studentMap.get(student.getUserId()).getName()
                    + "). Student IDs must be unique.");
        }
        students.add(student);
        studentMap.put(student.getUserId(), student);
    }

    public boolean studentIdExists(int studentId) {
        return studentMap.containsKey(studentId);
    }

    public Student getStudentById(int studentId) throws StudentNotFoundException {
        Student student = studentMap.get(studentId);
        if (student == null) {
            throw new StudentNotFoundException(studentId);
        }
        return student;
    }

    public List<Student> getAllStudents() {
        return Collections.unmodifiableList(students);
    }

    public int getTotalStudents() {
        return students.size();
    }
}
