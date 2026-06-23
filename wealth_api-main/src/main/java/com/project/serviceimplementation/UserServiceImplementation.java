package com.project.serviceimplementation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.dto.AuthLogin;
import com.project.dto.UserDto;
import com.project.entities.User;
import com.project.enums.UserStatus;
import com.project.repository.UserRepository;
import com.project.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImplementation implements UserService {

	@Autowired
	UserRepository repo;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Override
	public UserDto createUser(UserDto dto) throws Exception {
		if (repo.existsByEmail(dto.getEmail())) {
			throw new Exception("A user is already registered with email: " + dto.getEmail());
		}
		User u = User.builder()
				.name(dto.getName())
				.role(dto.getRole())
				.email(dto.getEmail())
				.phone(dto.getPhone())
				.branchId(dto.getBranchId())
				.status(dto.getStatus() == null ? UserStatus.Active : dto.getStatus())
				.password(passwordEncoder.encode(dto.getPassword()))
				.build();
		User res = repo.save(u);
		log.info("User {} created ({})", res.getUserId(), res.getEmail());
		return toDto(res);
	}

	@Override
	public UserDto getUserById(Integer id) throws Exception {
		User user = repo.findById(id).orElseThrow(() -> new Exception("User not found with ID: " + id));
		return toDto(user);
	}

	@Override
	public List<UserDto> getAllUsers() {
		List<UserDto> dtos = new ArrayList<>();
		for (User user : repo.findAll()) {
			dtos.add(toDto(user));
		}
		return dtos;
	}

	@Override
	public UserDto updateUserStatus(Integer id, UserStatus status) throws Exception {
		User user = repo.findById(id).orElseThrow(() -> new Exception("User not found with ID: " + id));
		user.setStatus(status);
		User res = repo.save(user);
		log.info("User {} status -> {}", id, status);
		return toDto(res);
	}

	@Override
	public UserDto lockUser(Integer id) throws Exception {
		return updateUserStatus(id, UserStatus.Locked);
	}

	@Override
	public UserDto unlockUser(Integer id) throws Exception {
		return updateUserStatus(id, UserStatus.Active);
	}

	@Override
	public User authenticate(AuthLogin login) throws Exception {
		User user = repo.findByEmail(login.getEmail())
				.orElseThrow(() -> new Exception("Invalid email or password."));
		if (!passwordEncoder.matches(login.getPassword(), user.getPassword())) {
			throw new Exception("Invalid email or password.");
		}
		if (user.getStatus() == UserStatus.Locked) {
			throw new Exception("Account is locked.");
		}
		log.info("User {} authenticated", user.getEmail());
		return user;
	}

	/** Response DTO never carries the password hash. */
	private UserDto toDto(User u) {
		UserDto dto = new UserDto();
		dto.setUserId(u.getUserId());
		dto.setName(u.getName());
		dto.setEmail(u.getEmail());
		dto.setPhone(u.getPhone());
		dto.setBranchId(u.getBranchId());
		dto.setRole(u.getRole());
		dto.setStatus(u.getStatus());
		return dto;
	}
}
