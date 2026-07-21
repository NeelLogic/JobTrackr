package solannee.sheridancollege.ca.jobtrackr.controller;
import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import org.springframework.http.*;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
import solannee.sheridancollege.ca.jobtrackr.dto.application.*;import solannee.sheridancollege.ca.jobtrackr.model.*;import solannee.sheridancollege.ca.jobtrackr.service.*;
@RestController @RequestMapping("/api/applications") @RequiredArgsConstructor public class JobApplicationController{
 private final JobApplicationService service;private final CurrentUserService current;
 @GetMapping PageResponse<ApplicationResponse>list(Authentication a,@RequestParam(required=false)String search,@RequestParam(required=false)ApplicationStatus status,@RequestParam(required=false)EmploymentType employmentType,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(defaultValue="updatedAt")String sort,@RequestParam(defaultValue="desc")String direction){return service.list(current.require(a),search,status,employmentType,page,size,sort,direction);}
 @GetMapping("/{id}")ApplicationResponse get(Authentication a,@PathVariable Long id){return service.get(current.require(a),id);}
 @PostMapping ResponseEntity<ApplicationResponse>create(Authentication a,@Valid @RequestBody ApplicationRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(current.require(a),r));}
 @PutMapping("/{id}")ApplicationResponse update(Authentication a,@PathVariable Long id,@Valid @RequestBody ApplicationRequest r){return service.update(current.require(a),id,r);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)void delete(Authentication a,@PathVariable Long id){service.delete(current.require(a),id);}
}
