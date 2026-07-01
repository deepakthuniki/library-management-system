package com.library.model;

/**
 * A library member who can borrow books. Extends Person (inheritance).
 */
public class Member extends Person {

    public enum MemberType { STUDENT, FACULTY, GENERAL }
    public enum Status { ACTIVE, SUSPENDED, INACTIVE }

    private int memberId;
    private String membershipCode;
    private String email;
    private String phone;
    private String address;
    private MemberType memberType;
    private int maxBooksAllowed;
    private Status status;

    public Member(String fullName, String email, String phone, MemberType memberType) {
        super(fullName, email);
        this.email = email;
        this.phone = phone;
        this.memberType = memberType;
        this.maxBooksAllowed = defaultLimitFor(memberType);
        this.status = Status.ACTIVE;
    }

    // Full constructor used when hydrating from DB
    public Member(int memberId, String membershipCode, String fullName, String email, String phone,
                  String address, MemberType memberType, int maxBooksAllowed, Status status) {
        super(fullName, email);
        this.memberId = memberId;
        this.membershipCode = membershipCode;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.memberType = memberType;
        this.maxBooksAllowed = maxBooksAllowed;
        this.status = status;
    }

    private static int defaultLimitFor(MemberType type) {
        return switch (type) {
            case FACULTY -> 5;
            case STUDENT -> 3;
            case GENERAL -> 2;
        };
    }

    @Override
    public String describeRole() {
        return memberType.name() + " member";
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    // ----- Getters / Setters (Encapsulation: fields stay private) -----
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getMembershipCode() { return membershipCode; }
    public void setMembershipCode(String membershipCode) { this.membershipCode = membershipCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public MemberType getMemberType() { return memberType; }
    public void setMemberType(MemberType memberType) { this.memberType = memberType; }

    public int getMaxBooksAllowed() { return maxBooksAllowed; }
    public void setMaxBooksAllowed(int maxBooksAllowed) { this.maxBooksAllowed = maxBooksAllowed; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
