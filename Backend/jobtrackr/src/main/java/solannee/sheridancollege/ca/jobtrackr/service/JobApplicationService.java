package solannee.sheridancollege.ca.jobtrackr.service;
import jakarta.persistence.criteria.Predicate;import lombok.RequiredArgsConstructor;import org.springframework.data.domain.*;import org.springframework.data.jpa.domain.Specification;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.application.*;import solannee.sheridancollege.ca.jobtrackr.dto.dashboard.DashboardResponse;import solannee.sheridancollege.ca.jobtrackr.exception.ResourceNotFoundException;import solannee.sheridancollege.ca.jobtrackr.model.*;import solannee.sheridancollege.ca.jobtrackr.repository.JobApplicationRepository;
import java.time.LocalDate;import java.util.*;
@Service @RequiredArgsConstructor public class JobApplicationService{
 private static final Set<String>SORT_FIELDS=Set.of("company","jobTitle","applicationDate","status","createdAt","updatedAt","followUpDate");
 private final JobApplicationRepository repo;
 @Transactional(readOnly=true)public PageResponse<ApplicationResponse>list(User u,String search,ApplicationStatus status,EmploymentType type,int page,int size,String sort,String direction){
  String safeSort=SORT_FIELDS.contains(sort)?sort:"updatedAt";Sort.Direction dir="asc".equalsIgnoreCase(direction)?Sort.Direction.ASC:Sort.Direction.DESC;
  Specification<JobApplication>spec=(root,q,cb)->{List<Predicate>p=new ArrayList<>();p.add(cb.equal(root.get("user").get("id"),u.getId()));if(status!=null)p.add(cb.equal(root.get("status"),status));if(type!=null)p.add(cb.equal(root.get("employmentType"),type));if(search!=null&&!search.isBlank()){String s="%"+search.trim().toLowerCase()+"%";p.add(cb.or(cb.like(cb.lower(root.get("company")),s),cb.like(cb.lower(root.get("jobTitle")),s),cb.like(cb.lower(root.get("location")),s)));}return cb.and(p.toArray(Predicate[]::new));};
  return PageResponse.from(repo.findAll(spec,PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100),Sort.by(dir,safeSort))).map(this::map));
 }
 @Transactional(readOnly=true)public ApplicationResponse get(User u,Long id){return map(find(u,id));}
 @Transactional public ApplicationResponse create(User u,ApplicationRequest r){JobApplication a=new JobApplication();a.setUser(u);copy(r,a);return map(repo.save(a));}
 @Transactional public ApplicationResponse update(User u,Long id,ApplicationRequest r){JobApplication a=find(u,id);copy(r,a);return map(repo.save(a));}
 @Transactional public void delete(User u,Long id){repo.delete(find(u,id));}
 @Transactional(readOnly=true)public DashboardResponse dashboard(User u){LocalDate now=LocalDate.now();Map<String,Long>counts=new LinkedHashMap<>();for(ApplicationStatus s:ApplicationStatus.values())counts.put(s.name(),repo.countByUserIdAndStatus(u.getId(),s));return new DashboardResponse(repo.countByUserId(u.getId()),repo.countByUserIdAndApplicationDateBetween(u.getId(),now.withDayOfMonth(1),now.withDayOfMonth(now.lengthOfMonth())),counts.get("INTERVIEW"),counts.get("OFFER"),counts.get("REJECTED"),counts,repo.findTop5ByUserIdOrderByUpdatedAtDesc(u.getId()).stream().map(this::map).toList());}
 private JobApplication find(User u,Long id){return repo.findByIdAndUserId(id,u.getId()).orElseThrow(()->new ResourceNotFoundException("Application not found"));}
 private void copy(ApplicationRequest r,JobApplication a){if(r.salaryMin()!=null&&r.salaryMax()!=null&&r.salaryMin().compareTo(r.salaryMax())>0)throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");a.setCompany(r.company().trim());a.setJobTitle(r.jobTitle().trim());a.setLocation(trim(r.location()));a.setJobUrl(trim(r.jobUrl()));a.setApplicationDate(r.applicationDate());a.setStatus(r.status());a.setEmploymentType(r.employmentType());a.setSalaryMin(r.salaryMin());a.setSalaryMax(r.salaryMax());a.setSalaryCurrency(trim(r.salaryCurrency()));a.setNotes(trim(r.notes()));a.setFollowUpDate(r.followUpDate());}
 private String trim(String s){return s==null||s.isBlank()?null:s.trim();}
 private ApplicationResponse map(JobApplication a){return new ApplicationResponse(a.getId(),a.getCompany(),a.getJobTitle(),a.getLocation(),a.getJobUrl(),a.getApplicationDate(),a.getStatus(),a.getEmploymentType(),a.getSalaryMin(),a.getSalaryMax(),a.getSalaryCurrency(),a.getNotes(),a.getFollowUpDate(),a.getCreatedAt(),a.getUpdatedAt());}
}
