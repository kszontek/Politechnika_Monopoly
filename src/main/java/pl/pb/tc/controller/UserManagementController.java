package pl.pb.tc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.tc.domain.Role;
import pl.pb.tc.domain.User;
import pl.pb.tc.repository.UserRepository;

@Controller
@RequestMapping("/users")
public class UserManagementController {

    private final UserRepository userRepository;

    public UserManagementController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("activePage", "users");
        return "users/list";
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
