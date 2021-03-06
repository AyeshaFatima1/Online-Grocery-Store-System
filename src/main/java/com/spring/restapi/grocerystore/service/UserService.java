package com.spring.restapi.grocerystore.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.spring.restapi.grocerystore.configuration.MessageStrings;
import com.spring.restapi.grocerystore.dao.UserRepository;
import com.spring.restapi.grocerystore.dto.ResponseDto;
import com.spring.restapi.grocerystore.dto.SignInDto;
import com.spring.restapi.grocerystore.dto.SignInResponseDto;
import com.spring.restapi.grocerystore.dto.SignupDto;
import com.spring.restapi.grocerystore.dto.UserCreateDto;
import com.spring.restapi.grocerystore.dto.UserUpdateDto;
import com.spring.restapi.grocerystore.enums.ResponseStatus;
import com.spring.restapi.grocerystore.enums.Role;
import com.spring.restapi.grocerystore.exceptions.AuthenticationFailException;
import com.spring.restapi.grocerystore.exceptions.CustomException;
import com.spring.restapi.grocerystore.model.AuthenticationToken;
import com.spring.restapi.grocerystore.model.User;
import com.spring.restapi.grocerystore.utils.Helper;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@Service
public class UserService {

  	@Autowired
    UserRepository userRepository;

    @Autowired
    AuthenticationService authenticationService;

    Logger logger = LoggerFactory.getLogger(UserService.class);


    public ResponseDto signUp(SignupDto signupDto)  throws CustomException {
        // Check to see if the current email address has already been registered.
        if (Helper.notNull(userRepository.findByEmail(signupDto.getEmail()))) {
            // If the email address has been registered then throw an exception.
            throw new CustomException("User already exists");
        }
        // first encrypt the password
        String encryptedPassword = signupDto.getPassword();
        try {
            encryptedPassword = hashPassword(signupDto.getPassword());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.error("hashing password failed {}", e.getMessage());
        }


        User user = new User(signupDto.getFirstName(), signupDto.getLastName(), signupDto.getEmail(), Role.user, encryptedPassword );

        User createdUser;
        try {
            // save the User
            createdUser = userRepository.save(user);
            // generate token for user
            final AuthenticationToken authenticationToken = new AuthenticationToken(createdUser);
            // save token in database
            authenticationService.saveConfirmationToken(authenticationToken);
            // success in creating
            return new ResponseDto(ResponseStatus.success.toString(), encryptedPassword);
        } catch (Exception e) {
            // handle signup error
            throw new CustomException(e.getMessage());
        }
    }

    public SignInResponseDto signIn(SignInDto signInDto) throws CustomException {
        // first find User by email
        User user = userRepository.findByEmail(signInDto.getEmail());
        if(!Helper.notNull(user)){
            throw  new AuthenticationFailException("user not present");
        }
        try {
            // check if password is right
            if (!user.getPassword().equals(hashPassword(signInDto.getPassword()))){
                // passowrd doesnot match
                throw  new AuthenticationFailException(MessageStrings.WRONG_PASSWORD);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.error("hashing password failed {}", e.getMessage());
            throw new CustomException(e.getMessage());
        }

        AuthenticationToken token = authenticationService.getToken(user);

        if(!Helper.notNull(token)) {
            // token not present
            throw new CustomException("token not present");
        }

        return new SignInResponseDto ("success", token.getToken());
    }


    String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] digest = md.digest();
        String myHash = DatatypeConverter
                .printHexBinary(digest).toUpperCase();
        return myHash;
    }

    public User updateUser(User user) {
		return userRepository.save(user);
	}

}
