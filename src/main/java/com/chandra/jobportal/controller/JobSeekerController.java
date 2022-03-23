/**
 * 
 */
package com.chandra.jobportal.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chandra.jobportal.dao.CompanyDao;
import com.chandra.jobportal.dao.InterestedDao;
import com.chandra.jobportal.dao.JobPostingDao;
import com.chandra.jobportal.dao.JobSeekerDao;
import com.chandra.jobportal.dao.impl.JobSeekerDaoImpl;
import com.chandra.jobportal.entity.Company;
import com.chandra.jobportal.entity.Interested;
import com.chandra.jobportal.entity.JobApplication;
import com.chandra.jobportal.entity.JobPosting;
import com.chandra.jobportal.entity.JobPostingsView;
import com.chandra.jobportal.entity.JobSeeker;
import com.chandra.jobportal.mail.EmailServiceImpl;

/**
 * @author ashay
 *
 */
@Controller

@RequestMapping(value = "/")
public class JobSeekerController {

	@Autowired
	JobSeekerDao jobSeekerDao;

	@Autowired
	EmailServiceImpl emailService;
	
	@Autowired
	InterestedDao interestedDao;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * @param searchString
	 * @param locations
	 * @param companies
	 * @param salary
	 * @return Jobs that match the filter criteria
	 */
	@RequestMapping(value = "/searchjobs", method = RequestMethod.GET)
	public String searchJobs(@RequestParam("userId") String userId,
			@RequestParam("searchString") Optional<String> searchString,
			@RequestParam("locations") Optional<String> locations,
			@RequestParam("companies") Optional<String> companies, 
			@RequestParam("min") Optional<String> min,
			@RequestParam("max") Optional<String> max, Model model) {
		JobPostingsView jpv = new JobPostingsView();
		String search = "java";
		if (!searchString.equals(Optional.empty())) {
			search = searchString.get();
		}
		System.out.println("############ search:"+search);
		System.out.println("############ userId:"+userId);
		List<?> jobIds = jobSeekerDao.searchJobs(search);
		if ((!locations.equals(Optional.empty())) && (locations.get()!="")) {
			System.out.println("location");
			jpv.setLocation(locations.get());
		}
		if (!companies.equals(Optional.empty()) && companies.get()!="") {
			System.out.println("comp");
			jpv.setCompanyName(companies.get());
		}
		if (!min.equals(Optional.empty()) && !max.equals(Optional.empty())) {
		String salary = min.get()+","+max.get();
		jpv.setSalary(salary);
		}

		List<?> jp = jobSeekerDao.filterJobs(jpv, jobIds);

		JobSeeker jobseeker = jobSeekerDao.getJobSeeker(Integer.parseInt(userId));
		
		model.addAttribute("jobs", jp);
		model.addAttribute("seeker", jobseeker);
		
		return "jobsearch";
	}

	@Autowired
	CompanyDao companyDao;
	
	@Autowired
	JobPostingDao jobDao;
	
	@RequestMapping(value = "/showjob", method = RequestMethod.GET)
	public String showJob(@RequestParam("userId") String userId, @RequestParam("jobId") String jobId, Model model) {
		
		JobPosting job = jobDao.getJobPosting(Integer.parseInt(jobId));
		Company company = job.getCompany();
		JobSeeker seeker = jobSeekerDao.getJobSeeker(Integer.parseInt(userId));
		List<?> ij = interestedDao.getAllInterestedJobId(Integer.parseInt(userId));
		int i = 0,j=0;
		if(ij.contains(Integer.parseInt(jobId))){
			i = 1;
		}
		
		List<Integer> il = getAppliedJobs(userId);
		if(il.contains(Integer.parseInt(jobId))){
			j = 1;
		}

		
		System.out.println("############ showjob:"+job);
		model.addAttribute("job", job);
		model.addAttribute("seeker", seeker);
		model.addAttribute("company", company);
		model.addAttribute("interested", i);
		model.addAttribute("applied", j);
		
		return "userjobprofile";
	}



	/**
	 * @param name
	 * @param email
	 * @param password
	 * @param type
	 * @param model
	 * @return newly created job seeker
	 * @throws IOException
	 * @throws SQLException
	 */
	@RequestMapping(value = "/createuser", method = RequestMethod.POST)
	public String createJobSeeker(@RequestParam("name") String name, @RequestParam("email") String email,
			@RequestParam("password") String password, @RequestParam("type") String type, Model model)
			throws IOException, SQLException {

		int randomPIN = (int) (Math.random() * 9000) + 1000;
		String[] splited = name.split("\\s+");

		try {

			if (type.equals("seeker")) {

				JobSeeker j = new JobSeeker();
				j.setFirstName(splited[0]);
				j.setLastName(splited[1]);
				j.setPassword(password);
				j.setEmailId(email);
				j.setVerificationCode(randomPIN);
				j.setVerified(false);

				JobSeeker j1 = jobSeekerDao.createJobSeeker(j);

				String verificationUrl = "http://localhost:8080/register/verify?userId=" + j1.getJobseekerId() + "&pin="
						+ randomPIN + "&type=seeker";
				System.out.println("$$$$$$$$$$$ seeker  message:"  + j.toString());
				System.out.println("$$$$$$$$$$$ seeker  message email:"  + email+"; verificationUrl:"+verificationUrl);
				emailService.sendSimpleMessage(email, "Verification Pin", verificationUrl);
				model.addAttribute("name", j1.getFirstName());
				return "codesent";

			}

			else {

				Company c = new Company();
				c.setVerified(false);
				c.setVerificationCode(randomPIN);
				c.setCompanyName(name);
				c.setCompanyUser(email);
				c.setPassword(password);
				c.setHeadquarters("head");

				Company c1 = companyDao.createCompany(c);

				String verificationUrl = "http://localhost:8080/register/verify?userId=" + c1.getCompanyId() + "&pin="
						+ randomPIN + "&type=recruiter";
			       System.out.println("$$$$$$$$$$$   message:"  + c.toString());
				emailService.sendSimpleMessage(email, "Verification Pin", verificationUrl);
				model.addAttribute("name", c1.getCompanyName());

				// Company c1 =companyDao.
				return "codesent";
			}

		} catch (SQLException se) {
			HttpHeaders httpHeaders = new HttpHeaders();
			Map<String, Object> message = new HashMap<String, Object>();
			Map<String, Object> response = new HashMap<String, Object>();
			message.put("code", "400");
			message.put("msg", "Email Already Exists");
			response.put("BadRequest", message);
			JSONObject json_test = new JSONObject(response);
			String json_resp = json_test.toString();

			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			return "error";

		} catch (Exception se) {
			HttpHeaders httpHeaders = new HttpHeaders();

			Map<String, Object> message = new HashMap<String, Object>();
			Map<String, Object> response = new HashMap<String, Object>();
			message.put("code", "400");
			message.put("msg", "Error Occured");
			response.put("BadRequest", message);
			JSONObject json_test = new JSONObject(response);
			String json_resp = json_test.toString();

			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			return "error";
		}
	}

	/**
	 * @param id
	 * @param model
	 * @return updated seeker view
	 */
	@RequestMapping(value = "/update", method = RequestMethod.GET)
	public String updateSeekerPage(@RequestParam("id") String id, Model model) {

		JobSeeker j1 = new JobSeekerDaoImpl().getJobSeeker(Integer.parseInt(id));
		model.addAttribute("j", j1);
		System.out.println("$$$$$$$$$$$ /update:"  + id);
		return "updateSeeker";
	}
	
	@RequestMapping(value = "/userprofile/{id}", method = RequestMethod.GET)
	public String showJobSeeker(@PathVariable("id") int id, Model model){
		
		JobSeeker jobseeker = jobSeekerDao.getJobSeeker(id);
		
		model.addAttribute("seeker", jobseeker);
		return "userprofile"; 
	}

	/**
	 * @param id
	 * @param firstname
	 * @param lastname
	 * @param emailid
	 * @param highesteducation
	 * @param password
	 * @param skills
	 * @param workex
	 * @param model
	 * @return updated userprofile view
	 * @throws Exception
	 */
	@RequestMapping(value = "/register/update", method = RequestMethod.POST)
	public String updateJobSeeker(@RequestParam("id") String id, @RequestParam("firstname") Optional<String> firstname,
			@RequestParam("lastname") Optional<String> lastname, @RequestParam("emailid") Optional<String> emailid,
			@RequestParam("highesteducation") Optional<String> highesteducation,
			@RequestParam("password") Optional<String> password, @RequestParam("skills") Optional<String> skills,
			@RequestParam("workex") Optional<String> workex, Model model) throws Exception {
		JobSeeker js = new JobSeeker();

		js.setJobseekerId(Integer.parseInt(id));
		System.out.println("$$$$$$$$$$$ /register/update:"  + id);
		if (!emailid.equals(Optional.empty())) {
			System.out.println("emailid done : " + emailid.get() + ":::: " + emailid);
			js.setEmailId(emailid.get());
		}
		if (!firstname.equals(Optional.empty())) {
			System.out.println("fname done");
			js.setFirstName(firstname.get());
		}
		if (!lastname.equals(Optional.empty())) {
			System.out.println("lname done");
			js.setLastName(lastname.get());
		}
		if (!highesteducation.equals(Optional.empty())) {
			System.out.println("highest edu");
			js.setHighestEducation(Integer.parseInt(highesteducation.get()));
		}
		if (!password.equals(Optional.empty())) {
			System.out.println("password");
			js.setPassword(password.get());
		}
		if (!skills.equals(Optional.empty())) {
			System.out.println("skills : " + skills);
			js.setSkills(skills.get());
			System.out.println("huhuhuh : " + skills.get());
		}

		if (!workex.equals(Optional.empty())) {
			System.out.println("workex : " + workex);
			js.setWorkEx(Integer.parseInt(workex.get()));
		}

		JobSeeker jobseeker = jobSeekerDao.getJobSeeker(Integer.parseInt(id));
		JobSeeker jobskr = null;
		if (jobseeker != null) {
			jobskr = jobSeekerDao.updateJobSeeker(js);
			System.out.println("updated");
		} else {
			jobskr = jobSeekerDao.createJobSeeker(js);
		}
		System.out.println("done");
	 
		System.out.println("Verification Code:"+jobskr.getVerificationCode());

		model.addAttribute("seeker", jobskr);
		return "userprofile";

	}

	
	/**
	 * @param id
	 * @param name
	 * @param headquarters
	 * @param user
	 * @param description
	 * @param model
	 * @return updated company
	 */
	@RequestMapping(value = "/update/company", method = RequestMethod.POST)
	public String companyupdate(@RequestParam("id") String id, @RequestParam("companyName") Optional<String> name,
			@RequestParam("headquarters") Optional<String> headquarters,
			@RequestParam("companyUser") Optional<String> user,
			@RequestParam("description") Optional<String> description, Model model) {

		Company c = new Company();

		c.setCompanyId(Integer.parseInt(id));

		if (!name.equals(Optional.empty())) {

			c.setCompanyName(name.get());
		}
		if (!user.equals(Optional.empty())) {

			c.setCompanyUser(user.get());
		}
		if (!headquarters.equals(Optional.empty())) {
			c.setHeadquarters(headquarters.get());
		}
		if (!description.equals(Optional.empty())) {
			c.setDescription(description.get());
		}

		Company company = companyDao.getCompany(Integer.parseInt(id));
		Company c1 = null;
		if (company != null) {
			c1 = companyDao.updateCompany(c);

		} else {
			return "error";
		}
		System.out.println("done");
		model.addAttribute("company", c1);
		return "companyprofile";

	}

	@RequestMapping(value = "/interested", method = RequestMethod.POST)
	public String createInterest(@RequestParam("userId") String userId, @RequestParam("jobId") String jobId, Model model) {

		try {
			Interested in = new Interested();
			in.setJobId(Integer.parseInt(jobId));
			in.setJobSeekerId(Integer.parseInt(userId));
			Interested i1 = interestedDao.createInterest(in);
			
		} catch (Exception e) {

			HttpHeaders httpHeaders = new HttpHeaders();

			Map<String, Object> message = new HashMap<String, Object>();
			Map<String, Object> response = new HashMap<String, Object>();
			message.put("code", "400");
			message.put("msg", "Error Occured");
			response.put("BadRequest", message);
			JSONObject json_test = new JSONObject(response);
			String json_resp = json_test.toString();

			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			return "error";

		}
		JobPosting job = jobDao.getJobPosting(Integer.parseInt(jobId));
		Company company = job.getCompany();
		JobSeeker seeker = jobSeekerDao.getJobSeeker(Integer.parseInt(userId));
		List<?> ij = interestedDao.getAllInterestedJobId(Integer.parseInt(userId));
		int i = 0, j = 0;
		if(ij.contains(Integer.parseInt(jobId))){
			i = 1;
		}
		String message="<div class=\"alert alert-success\">This job has been <strong>Successfully added</strong> to your interests</div>";
		
		List<Integer> il = getAppliedJobs(userId);
		if(il.contains(Integer.parseInt(jobId))){
			j = 1;
		}
		
		model.addAttribute("job", job);
		model.addAttribute("seeker", seeker);
		model.addAttribute("company", company);
		model.addAttribute("interested", i);
		model.addAttribute("message", message);
		model.addAttribute("applied", j);
		
		
		return "userjobprofile";
	}

	/**
	 * @param userId
	 * @param jobId
	 * @return "deleted" if the interest is deleted
	 */
	@RequestMapping(value = "/interested/delete", method = RequestMethod.POST)
	public String deleteInterest(@RequestParam("userId") String userId, @RequestParam("jobId") String jobId, Model model) {

		try {
			List<?> querylist = interestedDao.getInterestedJobId(Integer.parseInt(jobId), Integer.parseInt(userId));
			boolean interestDeleted = interestedDao.deleteInterest(Integer.parseInt(querylist.get(0).toString()));
			if (interestDeleted) {
				JobPosting job = jobDao.getJobPosting(Integer.parseInt(jobId));
				Company company = job.getCompany();
				JobSeeker seeker = jobSeekerDao.getJobSeeker(Integer.parseInt(userId));
				List<?> ij = interestedDao.getAllInterestedJobId(Integer.parseInt(userId));
				int i = 0;
				if(ij.contains(Integer.parseInt(jobId))){
					i = 1;
				}

				String message="<div class=\"alert alert-danger\">This job has been <strong>Successfully removed</strong> from your interests</div>";
				
				model.addAttribute("job", job);
				model.addAttribute("seeker", seeker);
				model.addAttribute("company", company);
				model.addAttribute("interested", i);
				model.addAttribute("message", message);
				model.addAttribute("applied", 1);
				
				
				return "userjobprofile";

			} else {
				return "error";
			}

		} catch (Exception e) {

			HttpHeaders httpHeaders = new HttpHeaders();

			Map<String, Object> message = new HashMap<String, Object>();
			Map<String, Object> response = new HashMap<String, Object>();
			message.put("code", "400");
			message.put("msg", "Error Occured");
			response.put("BadRequest", message);
			JSONObject json_test = new JSONObject(response);
			String json_resp = json_test.toString();

			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			return "error";

		}

	}

	/**
	 * @param jobSeekerId
	 * @return List of the jobs the job seeker is interested in
	 */
	@RequestMapping(value = "/getinterestedjobs", method = RequestMethod.GET)
	public String getInterestedJobsForJobSeeker(@RequestParam("jobSeekerId") String jobSeekerId, Model model) {
		
		JobSeeker jobseeker = jobSeekerDao.getJobSeeker(Integer.parseInt(jobSeekerId));
		List<?> jobSeekerInterestsList = jobSeekerDao.getJobSeeker(Integer.parseInt(jobSeekerId)).getInterestedjobs();
		
		model.addAttribute("jobs", jobSeekerInterestsList);
		model.addAttribute("seeker", jobseeker);
		return "interestedjobs";
	}
	
	/**
	 * @param jobSeekerId
	 * @return Job applications list for the job seeker
	 */
	@RequestMapping(value="/getappliedjobs", method = RequestMethod.GET)
	public List<Integer> getAppliedJobs(@RequestParam("jobSeekerId") String jobSeekerId){
		List<?> jobSeekerAppliedList =jobSeekerDao.getJobSeeker(Integer.parseInt(jobSeekerId)).getJobApplicationList();
		List<Integer> jobIdList = new ArrayList<Integer>();
		for (Iterator iterator = jobSeekerAppliedList.iterator(); iterator.hasNext();) {
			JobApplication ja = (JobApplication) iterator.next();
			int jobId = ja.getJobPosting().getJobId();
			jobIdList.add(jobId);
		}
		return jobIdList;
	}
	

	/**
	 * @param jobSeekerId
	 * @return Job applications list for the job seeker
	 */
	@RequestMapping(value="/sendmatchedjobs", method = RequestMethod.GET)
	public String getSendMatchedJobs(@RequestParam("jobSeekerId") String jobSeekerId, Model model){
		 
		String search = "java"; 
		List<?> jobIds = jobSeekerDao.searchJobs(search);
		
		JobSeeker js = jobSeekerDao.getJobSeeker(Integer.parseInt(jobSeekerId));
		String email = js.getEmailId();
		String subject = "Hi "+js.getFirstName() +" "+js.getLastName() +", Here is your matched jobs";
		System.out.println("$$$$$$$$$$$ seeker  sendJobdetails:"  +subject);
	 
		JobPostingsView jpv = new JobPostingsView();
	  
		List<?> jp = jobSeekerDao.filterJobs(jpv, jobIds);
		System.out.println("$$$$$$$$$$$ seeker  location:"  +jp.get(0).toString());
		model.addAttribute("jobs", jp);
		model.addAttribute("seeker", js);
		
		for (Object obj : jp) {
			//obj = (JobPostingsView)obj;
			//System.out.println("$$$$$$$$$$$ seeker  first job:"  +obj.title);
			//System.out.println("$$$$$$$$$$$ seeker  location:"  +object.getLocation());
		}
		
		for (Iterator iterator = jp.iterator(); iterator.hasNext();) {
			Object obj = (Object) iterator.next();
			System.out.println("$$$$$$$$$$$ seeker  second job:"  +obj.toString());
			//JobPostingsView object = (JobPostingsView) iterator.next();
			
			//System.out.println("$$$$$$$$$$$ seeker  second job:"  +obj.toString());
			//System.out.println("$$$$$$$$$$$ seeker second cname:"  +obj.getCompanyName());
		}
		//System.out.println("$$$$$$$$$$$ seeker  JobId:"  +model.getAttribute("jobs").toString());
		String text="<html><head> Matched jobs</head>"			 
				+ "<body id=\"pagetop\"> \r\n"
				+"<form class='form-inline row well' style='margin: 5px'	action=searchjobs method=get>"
				+"<input type=\"hidden\" name=\"userId\" value=\"${seeker.jobseekerId}\"></input>"
				+ "	<div class=\"container-fluid\"> \r\n"
				+ "		<div class=\"jumbotron\">\r\n"
				+ "			<div class=\"container text-center\"> \r\n"
				+ "				<h2>Here is your matched jobs based on your job selection</h2> \r\n"
				+ "			</div>\r\n"
				+ "		</div> \r\n"
				+ "		<div class=\"results\">\r\n"
				+ "			<h2>Matched Jobs :</h2>\r\n"
				+ "			<p>${fn:length(jobs)} search results</p>  \r\n"
				+ "			<c:forEach items=\"${jobs}\" var=\"job\">\r\n"
				+ "				<a class=\"a1\" href=\"sendmatchedjobs?userId=${seeker.jobseekerId}&jobId=${job[0]}\">${job[1]}</a>\r\n"
				+ "				<div class=\"row\">\r\n"
				+ "					<div class=\"col-sm-4 groups\">\r\n"
				+ "						<p>\r\n"
				+ "							<b>jobId:</b> ${job[0]}\r\n"
				+ "						</p>\r\n"
				+ "						<p>\r\n"
				+ "							<b>location:</b> ${job[4]}\r\n"
				+ "						</p>\r\n"
				+ "						<p>\r\n"
				+ "							<b>Salary:</b> $ ${job[5]}\r\n"
				+ "						</p>\r\n"
				+ "						\r\n"
				+ "						<p>\r\n"
				+ "							<b>Posted by:</b> ${job[8]}\r\n"
				+ "						</p>\r\n"
				+ "					</div>\r\n"
				+ "					<div class=\"col-sm-8\">\r\n"
				+ "						<p>\r\n"
				+ "							<b>Status:</b>\r\n"
				+ "							<c:if test=\"${job[7] == 0}\">\r\n"
				+ "								<c:out value=\"Open\" />\r\n"
				+ "							</c:if>\r\n"
				+ "							<c:if test=\"${job[7] == 1}\">\r\n"
				+ "								<c:out value=\"Filled\" />\r\n"
				+ "							</c:if>\r\n"
				+ "							<c:if test=\"${job[7] == 2}\">\r\n"
				+ "								<c:out value=\"Cancelled\" />\r\n"
				+ "							</c:if>\r\n"
				+ "						</p>\r\n"
				+ "						<p>\r\n"
				+ "							<b>Responsibilities :</b> ${job[3]}\r\n"
				+ "						</p>\r\n"
				+ "						<p>\r\n"
				+ "							<b>Description:</b> ${job[2]}\r\n"
				+ "						</p>\r\n"
				+ "					</div>\r\n"
				+ "				</div>\r\n"
				+ "				<hr />\r\n"
				+ "			</c:forEach>\r\n"
				+ "			 </div> "
				+"</form>"
				+ "</body></html>";
				
		//emailService.sendSimpleMessage(email, "matched jobs", "sendJobdetails");
		//emailService.sendMessageWithInLineMsg(email, "matched jobs", text, "jobs");
		 System.out.println("$$$$$$$$$$$ calling sendInMail  job size:" +jp.size());
		//emailService.sendSelectedJobs(email, "matched jobs", jp) ;
		try {
			sendInMail(email,   jp);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "sendjobdetails";
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/sendmatchedjobs1", method = RequestMethod.POST)
	@ResponseBody
	public String sendInMail(  String email, @RequestBody List<?> jobs)throws MessagingException{ 
		/*
		 * String search = "java"; List<?> jobIds = jobSeekerDao.searchJobs(search);
		 * 
		 * JobSeeker js = jobSeekerDao.getJobSeeker(Integer.parseInt(jobSeekerId));
		 * String email = js.getEmailId(); String subject = "Hi "+js.getFirstName()
		 * +" "+js.getLastName() +", Here is your matched jobs";
		 * System.out.println("$$$$$$$$$$$ seeker  sendJobdetails:" +subject);
		 * 
		 * JobPostingsView jpv = new JobPostingsView();
		 * 
		 * List<?> jp = jobSeekerDao.filterJobs(jpv, jobIds);
		 */
		 System.out.println("$$$$$$$$$$$ calling sendInMail  email:" +email);
		 System.out.println("$$$$$$$$$$$ from sendInMail  jobs size:" +jobs.size());
	 
		emailService.sendSelectedJobs(email, "matched jobs", jobs) ;
		return "sendjobdetails";
	}	

}