
	package com.chandra.jobportal.dao.impl;

	import java.util.ArrayList;
	import java.util.Iterator;
	import java.util.List;

	import javax.persistence.EntityManager;
	import javax.persistence.PersistenceContext;
	import javax.persistence.Query;

	import org.springframework.stereotype.Repository;
	import org.springframework.stereotype.Service;
	import org.springframework.transaction.annotation.Transactional;

	import com.chandra.jobportal.dao.InterestedDao;
import com.chandra.jobportal.dao.InterviewDao;
import com.chandra.jobportal.entity.Interested;
import com.chandra.jobportal.entity.Interview;
import com.chandra.jobportal.entity.JobPosting;

	
	 
	@Repository
	@Transactional
	@Service
	public class InterviewDaoImpl implements InterviewDao {

		@PersistenceContext
		private EntityManager entityManager;

		@Override
		public Interview createInterview(int jobseekerid, String company, String location, String datetime, String flag) {
			Interview interview = new Interview();
			interview.setCompany(company);
			interview.setJobseekerid(jobseekerid);
			interview.setDatetime(datetime);
			interview.setLocation(location);
			interview.setFlag("false");
			entityManager.merge(interview);
			
			return interview;
		}

		
		@Override
		public String acceptInterview(int jobseekerid) {
			Interview interview = new Interview();
			interview.setFlag("true");
			Query query = entityManager.createQuery("UPDATE interview SET flag = true WHERE jobseekerid= :id");
			query.setParameter("id", jobseekerid);
			entityManager.merge(interview);
			return "updated";
		}


		@Override
		public List<Interview> getAllInterviews(int jobseekerid) {
			
			//Query query = entityManager.createQuery("SELECT company, location, time FROM interview WHERE jobseekerid = :jobseekerid");
			return null;
		}

		
		

	}


