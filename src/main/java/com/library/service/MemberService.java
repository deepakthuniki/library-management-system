package com.library.service;

import com.library.dao.MemberDao;
import com.library.exception.DuplicateRecordException;
import com.library.exception.RecordNotFoundException;
import com.library.model.Member;
import com.library.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);
    private final MemberDao memberDao;

    public MemberService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    public Member registerMember(String fullName, String email, String phone,
                                  Member.MemberType type) throws SQLException, DuplicateRecordException {
        if (!ValidationUtil.isNonBlank(fullName)) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("Invalid phone number: " + phone);
        }
        if (memberDao.findByEmail(email).isPresent()) {
            throw new DuplicateRecordException("A member with email " + email + " already exists");
        }
        Member member = new Member(fullName, email, phone, type);
        Member saved = memberDao.save(member);
        log.info("Member registered: {} <{}>", fullName, email);
        return saved;
    }

    public Member getMember(int memberId) throws SQLException, RecordNotFoundException {
        return memberDao.findById(memberId)
                .orElseThrow(() -> new RecordNotFoundException("No member found with ID " + memberId));
    }

    public List<Member> listAll() throws SQLException {
        return memberDao.findAll();
    }

    public List<Member> search(String query) throws SQLException {
        return memberDao.search(query);
    }

    public void updateMember(Member member) throws SQLException, RecordNotFoundException {
        if (!memberDao.update(member)) {
            throw new RecordNotFoundException("Cannot update - member not found: " + member.getMemberId());
        }
    }

    public void suspendMember(int memberId) throws SQLException, RecordNotFoundException {
        Member m = getMember(memberId);
        m.setStatus(Member.Status.SUSPENDED);
        updateMember(m);
        log.info("Member suspended: {}", memberId);
    }

    public void deleteMember(int memberId) throws SQLException, RecordNotFoundException {
        if (!memberDao.deleteById(memberId)) {
            throw new RecordNotFoundException("Cannot delete - member not found: " + memberId);
        }
    }

    public int totalMemberCount() throws SQLException {
        return memberDao.count();
    }
}
