package com.example.demo;

import com.cloudinary.utils.ObjectUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    CloudinaryConfig cloudc;

    @Autowired
    private UserService userService;

    @RequestMapping("/")
    public String listMessages(Model model)
    {
        model.addAttribute("messages", messageRepository.findAllByOrderById());
        return "list";
    }

    @GetMapping("/login")
    public String login(Model model)
    {
        return "login";
    }

    /**
     * for new sign up
     * @param model
     * @return
     */
    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String showRegistrationPage(Model model)
    {
        model.addAttribute("user", new User());
        return "registration";
    }

    /**
     * for new sign up
     * @param user
     * @param result
     * @param model
     * @param file
     * @return
     */
    @RequestMapping(value="/register", method=RequestMethod.POST)
    public String processRegistrationPage(@Valid @ModelAttribute("user") User user, BindingResult result, Model model,
                                          @RequestParam("file") MultipartFile file)
    {
        model.addAttribute("user", user);
        if (result.hasErrors())
        {
            return "registration";
        }
        else
        {
            userService.saveUser(user, file);
            model.addAttribute("message", "User Account Successfully Created");
        }
        return "redirect:/";
    }

    /**
     * add new messge
     * @param model
     * @param request
     * @param authentication
     * @param principal
     * @return
     */
    @GetMapping("/add")
    public String addMessage(Model model, HttpServletRequest request, Authentication authentication, Principal principal)
    {
        model.addAttribute("message", new Message());
        return "messageform";
    }

    /**
     * add new message
     * @param message
     * @param result
     * @param file
     * @param request
     * @param authentication
     * @param principal
     * @return
     */
    @PostMapping("/add")
    public String processMessage(@Valid @ModelAttribute("message") Message message, BindingResult result,
                                 @RequestParam("file") MultipartFile file, HttpServletRequest request, Authentication authentication, Principal principal)
    {
        Boolean isAdmin = request.isUserInRole("ADMIN");
        Boolean isUser = request.isUserInRole("USER");
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        String username = principal.getName();
        if(result.hasErrors())
        {
            return "messageform";
        }
        if(file.isEmpty())
        {
            messageRepository.save(message);
            message.setUser(userRepository.findByUsername(username));
            messageRepository.save(message);
        }
        else {
            try {
                Map uploadResult = cloudc.upload(file.getBytes(), ObjectUtils.asMap("resourcetype", "auto"));
                message.setImage(uploadResult.get("url").toString());
                messageRepository.save(message);
                message.setUser(userRepository.findByUsername(username));
                messageRepository.save(message);
            } catch (IOException e) {
                e.printStackTrace();
                return "redirect:/add";
            }
            messageRepository.save(message);
        }

        return "redirect:/";
    }

    /**
     * update user profile
     * @param model
     * @param request
     * @param authentication
     * @param principal
     * @return registration
     */
    @RequestMapping("/viewUser")
    public String viewUser(Model model, HttpServletRequest request, Authentication authentication, Principal principal)
    {
        Boolean isAdmin = request.isUserInRole("ADMIN");
        Boolean isUser = request.isUserInRole("USER");
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        String username = principal.getName();
        model.addAttribute("user", userRepository.findByUsername(username));
        return "registration";
    }

    /**
     * update messge by only message owner and admin
     * @param id
     * @param model
     * @param request
     * @param authentication
     * @param principal
     * @return
     */
    @RequestMapping("/update/{id}")
    public String updateMessage(@PathVariable("id") long id, Model model, HttpServletRequest request, Authentication authentication, Principal principal)
    {
        Boolean isAdmin = request.isUserInRole("ADMIN");
        Boolean isUser = request.isUserInRole("USER");
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        String username = principal.getName();
        if(username.equals(messageRepository.findById(id).get().getUser().getUsername())|| username.equalsIgnoreCase("ADMIN")) {
            model.addAttribute("message", messageRepository.findById(id));
            return "messageform";
        }
        else
        {
            return "redirect:/";
        }
    }

    /**
     * delete message by only message owner and admin
     * @param id
     * @param request
     * @param authentication
     * @param principal
     * @return
     */
    @RequestMapping("/delete/{id}")
    public String deleteMessage(@PathVariable("id") long id, HttpServletRequest request, Authentication authentication, Principal principal)
    {
        Boolean isAdmin = request.isUserInRole("ADMIN");
        Boolean isUser = request.isUserInRole("USER");
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();
        String username = principal.getName();
        if(username.equals(messageRepository.findById(id).get().getUser().getUsername())|| username.equalsIgnoreCase("ADMIN"))
        {
            messageRepository.deleteById(id);
        }
        return "redirect:/";
    }

    /**
     * display all of the users
     * @param model
     * @return go to list of users page
     */
    @RequestMapping("/showuserprofile")
    public String showUserList(Model model)
    {
        model.addAttribute("users", userRepository.findAll());
        return "userprofile";
    }

}
