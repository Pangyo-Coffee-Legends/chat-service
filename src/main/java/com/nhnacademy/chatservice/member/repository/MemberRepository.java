package com.nhnacademy.chatservice.member.repository;

import com.nhnacademy.chatservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMbEmail(String mbEmail);

    /**
     * 특정 채팅방에 참여하지 않은 멤버 목록을 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @return 해당 채팅방에 참여하지 않은 멤버 목록
     */
    @Query("SELECT m FROM Member m WHERE m.id NOT IN " +
            "(SELECT cp.member.id FROM ChatParticipant cp WHERE cp.chatRoom.id = :roomId)")
    List<Member> findMembersNotInChatRoom(@Param("roomId") Long roomId);
}
