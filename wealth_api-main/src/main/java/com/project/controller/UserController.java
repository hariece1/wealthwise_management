package com.project.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.dto.ResponseMsg;
import com.project.dto.UserDto;
import com.project.enums.UserStatus;
import com.project.service.UserService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

	@Autowired
	UserService userService;

	@PostMapping("create")
	public ResponseEntity<?> create(@RequestBody UserDto userDto) {
		try {
			return new ResponseEntity<>(userService.createUser(userDto), HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(new ResponseMsg(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	@GetMapping
	public ResponseEntity<List<UserDto>> getAll() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(@PathVariable Integer id) {
		try {
			return ResponseEntity.ok(userService.getUserById(id));
		} catch (Exception e) {
			return new ResponseEntity<>(new ResponseMsg(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestParam UserStatus status) {
		try {
			return ResponseEntity.ok(userService.updateUserStatus(id, status));
		} catch (Exception e) {
			return new ResponseEntity<>(new ResponseMsg(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	@PatchMapping("/{id}/lock")
	public ResponseEntity<?> lock(@PathVariable Integer id) {
		try {
			return ResponseEntity.ok(userService.lockUser(id));
		} catch (Exception e) {
			return new ResponseEntity<>(new ResponseMsg(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	@PatchMapping("/{id}/unlock")
	public ResponseEntity<?> unlock(@PathVariable Integer id) {
		try {
			return ResponseEntity.ok(userService.unlockUser(id));
		} catch (Exception e) {
			return new ResponseEntity<>(new ResponseMsg(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}
}
