package com.test.demo.user.service;

import com.test.demo.common.domain.exception.CertificationCodeNotMatchedException;
import com.test.demo.common.domain.exception.ResourceNotFoundException;
import com.test.demo.mock.FakeMailSender;
import com.test.demo.mock.FakeUserRepository;
import com.test.demo.mock.TestClockHolder;
import com.test.demo.mock.TestUuidHolder;
import com.test.demo.user.domain.User;
import com.test.demo.user.domain.enums.UserStatus;
import com.test.demo.user.domain.request.UserCreateDto;
import com.test.demo.user.domain.request.UserUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class UserServiceImplTest {

    private UserServiceImpl userServiceImpl;

    @BeforeEach
    void init() {
        FakeMailSender fakeMailSender = new FakeMailSender();
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        userServiceImpl = UserServiceImpl.builder()
                .userRepository(fakeUserRepository)
                .uuidHolder(new TestUuidHolder("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .clockHolder(new TestClockHolder(1678530673958L))
                .certificationServiceImpl(new CertificationServiceImpl(fakeMailSender))
                .build();
        fakeUserRepository.save(User.builder()
                .id(1L)
                .email("tester@test.com")
                .nickname("tester")
                .address("Seoul")
                .status(UserStatus.ACTIVE)
                .lastLoginAt(1678530673958L)
                .certificationCode("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .build());
        fakeUserRepository.save(User.builder()
                .id(2L)
                .email("tester2@test.com")
                .nickname("tester2")
                .address("Jeju")
                .status(UserStatus.PENDING)
                .lastLoginAt(1678530673958L)
                .certificationCode("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .build());
    }

    @Test
    void getByEamil은_ACTIVE_상태인_유저를_찾아올_수_있다() {
        // given
        String email = "tester@test.com";

        // when
        User user = userServiceImpl.getByEmail(email);

        // then
        assertThat(user.getEmail()).isEqualTo(email);
    }

    @Test
    void getByEmail은_PENDING_상태인_유저를_찾아올_수_없다() {
        // given
        String email = "tester2@test.com";

        // when
        // then
        assertThatThrownBy(() -> {
            User user = userServiceImpl.getByEmail(email);
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById는_ACTIVE_상태인_유저를_찾아올_수_있다() {
        // given
        // when
        User user = userServiceImpl.getById(1);

        // then
        assertThat(user.getId()).isEqualTo(1);
    }

    @Test
    void getById는_PENDING_상태인_유저를_찾아올_수_없다() {
        // given
        // when
        // then
        assertThatThrownBy(() -> {
            User user = userServiceImpl.getById(2);
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void userCreateDto를_이용하여_유저_정보를_생성할_수_있다() {
        // given
        UserCreateDto userCreateDto = UserCreateDto.builder()
                .email("tester3@test.com")
                .nickname("tester3")
                .address("Seoul")
                .build();

        // when
        User user = userServiceImpl.create(userCreateDto);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("tester3@test.com");
        assertThat(user.getNickname()).isEqualTo("tester3");
        assertThat(user.getAddress()).isEqualTo("Seoul");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void userUpdateDto를_이용하여_유저_정보를_수정할_수_있다() {
        // given
        UserUpdateDto userUpdateDto = UserUpdateDto.builder()
                .nickname("tester1")
                .address("Jeju")
                .build();

        // when
        userServiceImpl.update(1, userUpdateDto);

        // then
        User user = userServiceImpl.getById(1);
        assertThat(user.getId()).isNotNull();
        assertThat(user.getNickname()).isEqualTo("tester1");
        assertThat(user.getAddress()).isEqualTo("Jeju");
    }

    @Test
    void 사용자를_로그인_시키면_마지막_로그인_시간이_변경된다() {
        // given
        // when
        userServiceImpl.login(1);

        // then
        User user = userServiceImpl.getById(1);
        assertThat(user.getLastLoginAt()).isEqualTo(1678530673958L);
    }

    @Test
    void PENDING_상태의_사용자는_이메일_인증_코드로_ACTIVE_시킬_수_있다() {
        // given
        // when
        userServiceImpl.verifyEmail(2, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // then
        User user = userServiceImpl.getById(2);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void PENDING_상태의_사용자가_이메일_인증_코드를_잘못_입력한_경우_에러를_반환한다() {
        // given
        // when
        // then
        assertThatThrownBy(() -> {
            userServiceImpl.verifyEmail(2, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab");
        }).isInstanceOf(CertificationCodeNotMatchedException.class);
    }

}
