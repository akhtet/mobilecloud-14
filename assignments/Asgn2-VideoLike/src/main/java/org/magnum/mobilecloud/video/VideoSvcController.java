/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

@Controller
public class VideoSvcController {
	
	public static final String TITLE_PARAMETER = "title";

	public static final String DURATION_PARAMETER = "duration";
	
	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	@Autowired
	private VideoRepository videos;
	
	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to conver the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.
	//
	// The VIDEO_SVC_PATH is set to "/video" in the VideoSvcApi
	// interface. We use this constant to ensure that the 
	// client and service paths for the VideoSvc are always
	// in synch.
	//
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		videos.save(v);
		
		v.setLikes(0);
		if (v.getUrl().length() == 0) 
			v.setUrl(VideoSvcApi.VIDEO_SVC_PATH + '/' + Long.toString(v.getId()));

		videos.save(v);
		
		System.out.println("AUNG: Added video url " + v.getUrl());
		return v;
	}
	
	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return Lists.newArrayList(videos.findAll());
		//return videos.findAll();
		/*
		Set<Video> vSet = new HashSet<Video>();
		
		for (Iterator<Video> vIter=videos.findAll().iterator(); vIter.hasNext() ;) {
				Video v = new Video(vIter.next());
				vSet.add(v);
				System.out.println("AUNG: Returning video " + v.getName() + "|" + v.getUrl() +  "|" + Long.toString(v.getDuration()));
		}
		System.out.println("AUNG: Returned video count " + Integer.toString(vSet.size()) );
		return vSet;
		*/
	}

	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(@PathVariable("id") long id, HttpServletResponse resp) throws IOException
	{		
		
		Video v = videos.findOne(id);
		
		if (v == null) 
			resp.sendError(404, "Video id does not exist");
		return v;
	}

	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method=RequestMethod.POST)
	public @ResponseBody void likeVideo(@PathVariable("id") long id, Principal p, HttpServletResponse resp) throws IOException
	{		
		Video v = videos.findOne(id);
		
		System.out.println("AUNG: Liking video " + Long.toString(id));
		
		if (v == null) { 
			resp.sendError(404, "Video id does not exist");
		}
		else {
			Set<String> likedUsers = v.getLikedUsers();
			if (!likedUsers.contains(p.getName())) {
				likedUsers.add(p.getName());
				v.setLikedUsers(likedUsers);
				v.setLikes(v.getLikes() + 1);
				videos.save(v);
				resp.setStatus(HttpServletResponse.SC_OK);
			}
			else {
				resp.sendError(400, "Video is already liked");				
			}
		}
	}	
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method=RequestMethod.POST)
	public  @ResponseBody void unlikeVideo(@PathVariable("id") long id, Principal p, HttpServletResponse resp) throws IOException
	{		
		Video v = videos.findOne(id);
	
		System.out.println("AUNG: Unliking video " + Long.toString(id));
		
		
		if (v == null) {
			resp.sendError(404, "Video id does not exist");
		}
		else {
			Set<String> likedUsers = v.getLikedUsers();

			if (likedUsers.contains(p.getName())) {
				likedUsers.remove(p.getName());
				v.setLikedUsers(likedUsers);
				v.setLikes(v.getLikes() - 1);
				videos.save(v);
				resp.setStatus(HttpServletResponse.SC_OK);
			}
			else {
				resp.sendError(400, "Video has not been liked before");
			}
		}
	}	


	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Set<String> getLikedBy(@PathVariable("id") long id, HttpServletResponse resp) throws IOException
	{		
		Video v = videos.findOne(id);
		
		if (v == null) 
			resp.sendError(404, "Video id does not exist");
		
		Set<String> likedUsers = v.getLikedUsers();
		return likedUsers;
	}	
	
	// Receives GET requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(TITLE_PARAMETER) String title
	){
		return videos.findByName(title);
	}	
	
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDuration(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(DURATION_PARAMETER) long maxduration
	){
		return videos.findByDurationLessThan(maxduration);
	}		
	
	
}
