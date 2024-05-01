package com.example.Software.project.Controller.Auth;

import com.example.Software.project.Controller.Auth.Response.UserInfoResponse;
import jakarta.validation.constraints.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.Software.project.Entity.Forgetpass.UpdatePasswordRequest;
import com.example.Software.project.Entity.Login.AppUser;
import com.example.Software.project.Entity.Login.LogRole;
import com.example.Software.project.Entity.Login.Role;
import com.example.Software.project.Repo.Login.AppUserRepo;
import com.example.Software.project.Repo.Login.RoleRepo;
import com.example.Software.project.config.JwtUtils;
import com.example.Software.project.config.UserDetailsImpl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;


import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
//@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthonticationController {

    private final AuthenticationManager authenticationManager;

    final
    AppUserRepo userRepository;

    final
    RoleRepo roleRepository;

    private final PasswordEncoder encoder;

    final
    JwtUtils jwtUtils;

    private final Map<String, String> otpStorage = new HashMap<>();


    private final JavaMailSender emailSender;

    @Autowired
    public AuthonticationController(AuthenticationManager authenticationManager, AppUserRepo userRepository, RoleRepo roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils, JavaMailSender emailSender) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.emailSender = emailSender;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthonticationRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        //GrantedAuthority::getAuthority


        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new UserInfoResponse(userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        AppUser user = new AppUser(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(LogRole.USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(LogRole.ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(LogRole.ADMIN_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);

                        break;
                    default:
                        Role userRole = roleRepository.findByName(LogRole.USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }




    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest) {
        Optional<AppUser> optionalUser = userRepository.findByEmail(updatePasswordRequest.getEmail());
        if (optionalUser.isPresent()) {
            AppUser user = optionalUser.get();
            if (validateOTP(updatePasswordRequest.getEmail(), updatePasswordRequest.getOtp())) {
                user.setPassword(encoder.encode(updatePasswordRequest.getNewPassword()));
                userRepository.save(user);
                return ResponseEntity.ok(new MessageResponse("Password updated successfully!"));
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid OTP!"));
            }
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("User not found with email: " + updatePasswordRequest.getEmail()));
        }
    }

    private boolean validateOTP(String email, String otp) {
        String storedOTP = otpStorage.get(email);
        return storedOTP != null && storedOTP.equals(otp);
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthonticationController.class);
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody String email) {
//    String email1 = requestBody.get("email");
        String trimmedEmail = email.trim();
        logger.debug("Received request at /endpoint");
        // Generate OTP
        String otp = generateOTP();

        // Save OTP in storage
        otpStorage.put(trimmedEmail, otp);

        // Send OTP to user via email or SMS (not implemented)
        sendOTPEmail(trimmedEmail, otp);

        return ResponseEntity.ok(new MessageResponse("OTP sent successfully!"));
    }

//    private void sendOTPEmail(String email, String otp) {
//
//
//        // Trim whitespace from the email address
//        String trimmedEmail = email.trim();
//
//        // Validate email address format
////        if (!isValidEmail(trimmedEmail)) {
////            // Log error or throw an exception
////            // Handle the error gracefully
////            return;
////        }
//
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(trimmedEmail);
//        message.setSubject("Your OTP");
//        message.setText("Your OTP is: " + otp);
//
//        emailSender.send(message);
//    }

    private void sendOTPEmail(String email, String otp) {

        String subject = "Your OTP";
        String text = "Your OTP is: " + otp;

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
//        String trimmedEmail = email.trim();
        try {
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(text);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        emailSender.send(message);
    }


    private boolean isValidEmail(String email) {
        // Implement email validation logic here
        // You can use regular expressions or libraries like Apache Commons Validator
        // Example using regular expression:
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }


    private String generateOTP() {
        Random random = new Random();
        int otpLength = 6;
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
