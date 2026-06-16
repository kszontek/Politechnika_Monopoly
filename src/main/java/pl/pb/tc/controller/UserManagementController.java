package pl.pb.tc.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.tc.domain.Role;
import pl.pb.tc.domain.User;
import pl.pb.tc.domain.UserProfile;
import pl.pb.tc.repository.UserRepository;

@Controller
@RequestMapping("/users")
public class UserManagementController {

    private static final String EMAIL_RE = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("activePage", "users");
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "users/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brak użytkownika id=" + id));
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "users/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("user") User user,
                       BindingResult bindingResult,
                       @RequestParam(required = false) Long id,
                       @RequestParam(required = false) String password,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (id == null) {
            String login = user.getUsername();
            String email = user.getEmail();

            if (login == null || login.length() < 3 || login.length() > 20 || !login.matches("^[a-z0-9]+$")) {
                bindingResult.rejectValue("username", "invalid", "Login: 3–20 znaków, tylko małe litery i cyfry");
            } else if (userRepository.existsByUsername(login)) {
                bindingResult.rejectValue("username", "taken", "Ten login jest już zajęty");
            }
            if (email == null || !email.matches(EMAIL_RE)) {
                bindingResult.rejectValue("email", "invalid", "Podaj poprawny adres e-mail");
            } else if (userRepository.existsByEmail(email)) {
                bindingResult.rejectValue("email", "taken", "Ten e-mail jest już zarejestrowany");
            }
            if (password == null || password.length() < 5) {
                bindingResult.rejectValue("password", "size", "Hasło musi mieć co najmniej 5 znaków");
            }
            if (user.getAge() < 18 || user.getAge() > 120) {
                bindingResult.rejectValue("age", "range", "Wymagany wiek 18–120 lat");
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("roles", Role.values());
                return "users/form";
            }

            user.setPassword(passwordEncoder.encode(password));
            user.attachProfile(new UserProfile());
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", "Dodano użytkownika: " + login);

        } else {
            if (user.getAge() < 18 || user.getAge() > 120) {
                bindingResult.rejectValue("age", "range", "Wymagany wiek 18–120 lat");
                model.addAttribute("roles", Role.values());
                return "users/form";
            }
            User existing = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Brak użytkownika id=" + id));
            existing.setAge(user.getAge());
            existing.setRole(user.getRole());
            userRepository.save(existing);
            redirectAttributes.addFlashAttribute("message", "Zapisano zmiany: " + existing.getUsername());
        }

        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.findById(id).ifPresent(u ->
                redirectAttributes.addFlashAttribute("message", "Usunięto użytkownika: " + u.getUsername()));
        userRepository.deleteById(id);
        return "redirect:/users";
    }

    @PostMapping("/{id}/promote")
    public String promote(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brak użytkownika id=" + id));
        if (user.getRole() == Role.USER) {
            user.setRole(Role.MODERATOR);
        } else if (user.getRole() == Role.MODERATOR) {
            user.setRole(Role.ADMIN);
        }
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "Zmieniono rangę: " + user.getUsername() + " → " + user.getRole().getDisplayName());
        return "redirect:/users";
    }
}
