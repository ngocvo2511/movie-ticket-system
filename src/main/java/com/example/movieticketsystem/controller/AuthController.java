package com.example.movieticketsystem.controller;


import com.example.movieticketsystem.model.Role;
import com.example.movieticketsystem.model.User;
import com.example.movieticketsystem.repository.RoleRepository;
import com.example.movieticketsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result, Model model) {
        // Validate user input
        if (result.hasErrors()) {
            return "register";
        }

        // Check if username exists
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("usernameError", "Username is already taken");
            return "register";
        }

        // Check if email exists
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("emailError", "Email is already in use");
            return "register";
        }

        // Set default role as customer
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Customer role not found"));
        user.setRole(customerRole);

        // Register user
        userService.registerNewUser(user);

        return "redirect:/login?registered";
    }
}