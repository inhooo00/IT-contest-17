package shop.itcontest17.itcontest17.multi.application;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;
import shop.itcontest17.itcontest17.multi.api.dto.response.MultiResDto;
import shop.itcontest17.itcontest17.quiz.domain.QuizScore;

@Service
@RequiredArgsConstructor
public class MultiService {

    private final MemberRepository memberRepository;
    private final Queue<Member> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, CompletableFuture<MultiResDto>> waitingUsers = new HashMap<>();

    public CompletableFuture<MultiResDto> addToQueue(String email) {
        CompletableFuture<MultiResDto> future = new CompletableFuture<>();
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        // 사용자 대기열에 추가
        waitingUsers.put(member.getEmail(), future);
        waitingQueue.add(member);

        // 두 명이 모이면 매칭 실행
        if (waitingQueue.size() >= 2) {
            Member user1 = waitingQueue.poll();
            Member user2 = waitingQueue.poll();

            String roomId = createRoomId();

            // 매칭된 상대방 이름 설정
            MultiResDto resultForUser1 = new MultiResDto(roomId, user1.getEmail());
            MultiResDto resultForUser2 = new MultiResDto(roomId, user2.getEmail());

            // 매칭된 사용자에게 결과 반환
            waitingUsers.get(user1.getEmail()).complete(resultForUser1);
            waitingUsers.get(user2.getEmail()).complete(resultForUser2);

            // 매칭 완료 후 상태 정리
            waitingUsers.remove(user1.getEmail());
            waitingUsers.remove(user2.getEmail());
        }

        return future;
    }

    private String createRoomId() {
        return UUID.randomUUID().toString(); // 고유한 roomId 생성
    }

    @Transactional
    public boolean winnerProcessing(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        member.incrementStreak(QuizScore.MULTI_SCORE.getScore());

        return true;
    }
}