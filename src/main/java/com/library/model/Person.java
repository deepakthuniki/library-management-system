package com.library.model;

/**
 * Abstract base for any human entity in the system (Member, User/Staff).
 * Demonstrates INHERITANCE: shared identity fields + behavior live here,
 * subclasses (Member, User) extend and specialize.
 */
public abstract class Person {

    protected String fullName;
    protected String contactInfo; // email or phone depending on subclass

    protected Person(String fullName, String contactInfo) {
        this.fullName = fullName;
        this.contactInfo = contactInfo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be blank");
        }
        this.fullName = fullName;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    /**
     * POLYMORPHISM: each subclass defines what "role description" means for it
     * (e.g. Member -> membership type, User -> staff role).
     */
    public abstract String describeRole();

    @Override
    public String toString() {
        return fullName + " (" + describeRole() + ")";
    }
}
