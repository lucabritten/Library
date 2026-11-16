package de.htwsaar.library.member;

import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "MEMBER")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq_gen")
    @SequenceGenerator(name = "member_seq_gen", sequenceName = "member_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean isLocked;

    protected Member() {
    }

    public Member(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        role = Role.USER;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isLocked() {
        return isLocked;
    }

    // member/Member.java
    public Set<String> getScopes() {
        return role.scopes();
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", isLocked=" + isLocked +
                '}';
    }
}
