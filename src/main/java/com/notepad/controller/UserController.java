package com.notepad.controller;

import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.notepad.entity.enumeration.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.notepad.dto.TokenAndPasswordDTO;
import com.notepad.dto.UserDTO;
import com.notepad.entity.User;
import com.notepad.error.BadRequestAlertException;
import com.notepad.repository.UserRepository;
import com.notepad.service.MailService;
import com.notepad.service.UserService;

/**
 * REST controller for managing {@link User}
 */
@RestController
@CrossOrigin
public class UserController {

	private final Logger log = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MailService mailService;

	/**
	 * {@code GET  /auth/me} : get user by token.
	 * 
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the
	 *         logged in userDTO.
	 */
	@GetMapping("/auth/me")
	public ResponseEntity<UserDTO> getLoggedInUser(Principal principal) {
		log.info("Rest request to get information of logged in user by token!");
		String userName = principal.getName();
		UserDTO userDTO = userService.findByUserName(userName);
		return ResponseEntity.ok().body(userDTO);
	}

	/**
	 * {@code GET  /auth/makeUuid} : get UUID.
	 * 
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the UUID.
	 */
	@GetMapping("auth/makeUuid")
	public ResponseEntity<String> getUUID() {
		log.info("Rest request to generate random UUID.");
		String uuid = UUID.randomUUID().toString();
		UserDTO userDTORes = new UserDTO();
		UserDTO userDTO = new UserDTO();
		userDTO.setUserName(uuid);
		userDTO.setPassword(uuid);
		userDTO.setConfirmPassword(uuid);
		userDTO.setUserType(UserType.VISITOR);
		userDTO.setEmail(uuid+"@gmail.com");
		userDTORes = userService.save(userDTO);
		return ResponseEntity.ok().body(uuid);
	}

	/**
	 * {@code GET  /users} : get all the users.
	 * 
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
	 *         of users in body.
	 */
	@GetMapping("/users")
	public ResponseEntity<List<UserDTO>> getAllUsers() {
		log.info("Rest request to get all users");
		List<UserDTO> userDTOs = userService.findAll();
		return ResponseEntity.ok().body(userDTOs);
	}

	/**
	 * {@code POST /auth/retrievePwd} : send a password reset link to email id
	 * provided in param.
	 *
	 * @param email : the email id to send a password create link.
	 * @return the {@link ResponseEntity} with status {@code 200} and url generated
	 *         & sent to user.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 */
	@PostMapping("/auth/retrievePwd")
	public ResponseEntity<String> retrivePasswordSendMail(@RequestBody UserDTO userDTO, HttpServletRequest request) {
		log.info("Rest request to send mail on {} ", userDTO.getEmail());
		// check if user with email present or not?
		Optional<User> user = userRepository.findOneByEmail(userDTO.getEmail().toLowerCase());
		
		if (!user.isPresent()) {
			// TODO : make a custom exception for UserNotFound!
			throw new BadRequestAlertException("user not found with email : "+userDTO.getEmail(), null, null);
		} else {

			// generate & save token before sending a mail
			String token = UUID.randomUUID().toString();
			userService.saveTokenForUser(token, user.get());

			// generate URL for mail
			StringBuffer url = request.getRequestURL();
			String uri = request.getRequestURI();
			String ctx = request.getContextPath();
			String base = url.substring(0, url.length() - uri.length() + ctx.length()) + "/";

			String passwordResetURL = base + "setNewPassword?id=" + user.get().getUserId() + "&token=" + token;
			System.out.println("passwordResetURL: " + passwordResetURL);

			// send mail
			mailService.sendPasswordResetMail(user.get(), passwordResetURL);

		}

		return ResponseEntity.ok().body("mail sent succesfully to " + user.get().getEmail());
	}
	
	/**
	 * {@code POST /auth/resetPwd} : validate token and reset the password 
	 *
	 * @param TokenAndPasswordDTO : the token from URL in mail & new Password
	 * @return the {@link ResponseEntity} with status {@code 200} and return the updated user entity.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 */
	@PostMapping("/auth/resetPwd")
	public ResponseEntity<String> validateTokenAndResetPassword(@RequestBody TokenAndPasswordDTO tokenAndPasswordDTO) {
		log.info("Rest request to validate token and reset password");
		userService.resetPassword(tokenAndPasswordDTO);
		return ResponseEntity.ok().body("");
	}
	
}
