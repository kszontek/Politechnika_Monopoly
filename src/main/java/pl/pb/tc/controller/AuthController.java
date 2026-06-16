package pl.pb.tc.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.tc.dto.RegistrationForm;
import pl.pb.tc.service.UserService;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasFieldErrors("username") && userService.usernameTaken(form.getUsername())) {
            bindingResult.rejectValue("username", "taken", "Ten login jest juz zajety");
        }
        if (!bindingResult.hasFieldErrors("email") && userService.emailTaken(form.getEmail())) {
            bindingResult.rejectValue("email", "taken", "Ten e-mail jest juz zarejestrowany");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        userService.register(form);
        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
