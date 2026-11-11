package de.htwsaar.library.member;

import de.htwsaar.library.member.MemberDtos.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final static Logger logger =
            LoggerFactory.getLogger(MemberService.class);

    private final MemberRepo memberRepo;

    public MemberService(MemberRepo memberRepo) {
        this.memberRepo = memberRepo;
    }

    public MemberResponse findById(Long id) {
        return MemberResponse.from(require(id));
    }

    @Transactional
    public MemberResponse create(CreateMemberRequest req) {
        String normEmail = normalizeEmail(req.email());
        if (memberRepo.existsByEmail(normEmail))
            throw new MemberExceptions.MemberAlreadyExistsException(normEmail);
        Member saved = memberRepo.saveAndFlush(new Member(req.name(), normEmail));
        return MemberResponse.from(saved);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public Member require(Long id) {
        return memberRepo.findById(id)
                .orElseThrow(() -> {
                    logger.info("Member with id={} not found", id);
                    return new MemberExceptions.MemberNotFoundException(id);
                });
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepo.deleteById(id);
    }
}
