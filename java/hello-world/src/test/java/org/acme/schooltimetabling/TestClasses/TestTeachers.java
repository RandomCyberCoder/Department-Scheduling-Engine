package org.acme.schooltimetabling.TestClasses;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTeachers {
    @Test
    public void test_JUnit() {
        System.out.println("This is the testcase in this class");
        String str1="This is the testcase in this class";
        assertEquals("This is the testcase in this class", str1);
    }

    @Test
    public void test_JUnit2() {
        System.out.println("This is the testcase in this class");
        String str1="This is the testcase in this class";
        assertEquals("fefeThis is the testcase in this class", str1);
    }
}
