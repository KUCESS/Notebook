package com.kucess.notebook.controller;

import com.kucess.notebook.model.entity.*;
import com.kucess.notebook.model.io.ActivityIO;
import com.kucess.notebook.model.io.EmployeeIO;
import com.kucess.notebook.model.service.ActivityService;
import com.kucess.notebook.model.service.EmployeeService;
import com.kucess.notebook.model.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("${api.path}")
@RequiredArgsConstructor
public class HomePageController {

    private final UserService userService;

    private final EmployeeService employeeService;

    private final ActivityService activityService;

    @GetMapping("/welcome")
    @Transactional
    public String findUser(Authentication authentication, Model model){
        Person person = userService.getUserByUserName(authentication.getName());
        model.addAttribute("user", person);
        if (person.getAuthorityType() == AuthorityType.ADMIN){
            Set<Employee> employees = ((Admin) person).getEmployees();
            model.addAttribute("employees", employees);
        }else if(person.getAuthorityType() == AuthorityType.EMPLOYEE){
            List<Activity> activities = ((Employee) person).getActivities();
            model.addAttribute("activities", activities);
        }
        return "welcome";
    }

    @GetMapping("/employees")
    public String addEmployeesPages(Model model){
        EmployeeIO employeeIO = new EmployeeIO();
        model.addAttribute("employee", employeeIO);
        return "addEmployee";
    }

    @PostMapping("/employees")
    @Transactional
    public String registerEmployee(@ModelAttribute("employee") @Valid EmployeeIO employeeIO,
                                   BindingResult bindingResult,
                                   Authentication authentication
                                   ){
        if (bindingResult.hasErrors()){
            return "addEmployee";
        }
        String adminUserName = authentication.getName();
        employeeService.addEmployeeToAdmin(adminUserName, employeeIO);
        return "redirect:/notebook/v1/welcome";
    }

    @PostMapping(value = "/employees/{employeeUserName}")
    @Transactional
    public String removeEmployeeFromAdmin(@PathVariable("employeeUserName") String username, Authentication authentication){
        String adminUserName = authentication.getName();
        employeeService.removeEmployeeFromAdmin(adminUserName, username);
        return "redirect:/notebook/v1/welcome";
    }

    @PostMapping("/employees/insertCurrent")
    @Transactional
    public String addCurrentEmployee(@RequestParam("userName") String userName, Authentication authentication){
        try {
            employeeService.addEmployeeToAdmin(authentication.getName(), userName);
        }catch (UsernameNotFoundException usernameNotFoundException){
            usernameNotFoundException.printStackTrace();
        }
        return "redirect:/notebook/v1/welcome";
    }

    @GetMapping("/employees/{employeeUserName}/activities")
    public String getActivities(
            @PathVariable String employeeUserName,
            Authentication authentication,
            Model model
    ){
        String adminUserName = authentication.getName();
        List<ActivityIO> activityIOList =
                activityService.findActivityByAdminUserNameAndEmployeeUserName(adminUserName, employeeUserName);
        model.addAttribute("activities", activityIOList);
        model.addAttribute("empUserName", employeeUserName);
        return "activities";
    }

    @GetMapping("/employees/{employeeUserName}/activities/insert")
    public String addActivity(@PathVariable String employeeUserName, Model model){
        ActivityIO activityIO = new ActivityIO();
        model.addAttribute("activity", activityIO);
        model.addAttribute("employeeUserName", employeeUserName);
        return "insertActivity";
    }

    @PostMapping("/employees/{employeeUserName}/activities/insert")
    @Transactional
    public String addActivity(
            @ModelAttribute("activity") @Valid ActivityIO activityIO,
            BindingResult bindingResult,
            Authentication authentication,
            @PathVariable String employeeUserName
    ){
        if (bindingResult.hasErrors()){
            return "insertActivity";
        }
        String adminUserName = authentication.getName();
        if (activityIO.getId() == 0)
            activityService.addActivityToEmployee(adminUserName, employeeUserName, activityIO);
        else
            activityService.updateActivity(activityIO);
        return "redirect:/notebook/v1/employees/" + employeeUserName + "/activities";
    }

    @PostMapping("/employees/{empUserName}/activities/{actId}/delete")
    public String deleteActivity(@PathVariable String actId, @PathVariable String empUserName){
        activityService.deleteActivityFromEmployee(Long.parseLong(actId));
        return "redirect:/notebook/v1/employees/" + empUserName + "/activities";
    }

    @GetMapping("/employees/{empUserName}/activities/{actId}")
    public String getUpdatingActivity(@PathVariable String empUserName, @PathVariable String actId, Model model){
        ActivityIO activityById = activityService.findActivityById(Long.parseLong(actId));
        model.addAttribute("activity", activityById);
        model.addAttribute("employeeUserName", empUserName);
        return "insertActivity";
    }

}
