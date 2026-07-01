package com.library.dao;

import com.library.model.Member;
import com.library.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDao implements Dao<Member, Integer> {

    private static final Logger log = LoggerFactory.getLogger(MemberDao.class);

    @Override
    public Member save(Member m) throws SQLException {
        String code = generateMembershipCode();
        String sql = """
            INSERT INTO members (membership_code, full_name, email, phone, address,
                member_type, max_books_allowed, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, m.getFullName());
            ps.setString(3, m.getEmail());
            ps.setString(4, m.getPhone());
            ps.setString(5, m.getAddress());
            ps.setString(6, m.getMemberType().name());
            ps.setInt(7, m.getMaxBooksAllowed());
            ps.setString(8, m.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setMemberId(keys.getInt("MEMBER_ID"));
                }
            }
            m.setMembershipCode(code);
            log.info("Member registered: {} ({})", m.getFullName(), code);
            return m;
        }
    }

    private String generateMembershipCode() {
        return "MEM" + System.currentTimeMillis() % 1_000_000;
    }

    @Override
    public Optional<Member> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Member> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM members WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Member> findAll() throws SQLException {
        String sql = "SELECT * FROM members ORDER BY full_name";
        List<Member> members = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) members.add(mapRow(rs));
        }
        return members;
    }

    public List<Member> search(String nameQuery) throws SQLException {
        String sql = "SELECT * FROM members WHERE full_name LIKE ? OR email LIKE ? ORDER BY full_name";
        List<Member> members = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + nameQuery + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapRow(rs));
            }
        }
        return members;
    }

    /** Counts how many books a member currently has issued (not yet returned). */
    public int countActiveLoans(Connection conn, int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE member_id = ? AND status = 'ISSUED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public boolean update(Member m) throws SQLException {
        String sql = """
            UPDATE members SET full_name=?, email=?, phone=?, address=?, member_type=?,
                max_books_allowed=?, status=? WHERE member_id=?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getFullName());
            ps.setString(2, m.getEmail());
            ps.setString(3, m.getPhone());
            ps.setString(4, m.getAddress());
            ps.setString(5, m.getMemberType().name());
            ps.setInt(6, m.getMaxBooksAllowed());
            ps.setString(7, m.getStatus().name());
            ps.setInt(8, m.getMemberId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM members";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("member_id"),
                rs.getString("membership_code"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                Member.MemberType.valueOf(rs.getString("member_type")),
                rs.getInt("max_books_allowed"),
                Member.Status.valueOf(rs.getString("status"))
        );
    }
}
