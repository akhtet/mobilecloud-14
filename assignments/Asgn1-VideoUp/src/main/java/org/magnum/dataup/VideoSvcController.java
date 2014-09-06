
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import retrofit.client.Response;

@Controller
public class VideoSvcController  {

	public static final String DATA_PARAMETER = "data";

	public static final String ID_PARAMETER = "id";

	public static final String VIDEO_SVC_PATH = "/video";
	
	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	
	private List<Video> videos = new CopyOnWriteArrayList<Video>();
	
	private VideoFileManager vFM;

	private static final AtomicLong currentId = new AtomicLong(0L);
		
	private Map<Long,Video> videoMap = new HashMap<Long, Video>();

		
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList()
	{
		return videos;
	}
	
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v)
	{
				
		if(v.getId() == 0)
			v.setId(currentId.incrementAndGet());
		
		v.setDataUrl(getDataUrl(v.getId()));
		
		videos.add(v);
		videoMap.put(v.getId(), v);
			
		return v;
	}
	
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse resp ) throws IOException
	{
		if (videoMap.containsKey(id)) {
			
			Video v = videoMap.get(id);
			vFM = VideoFileManager.get();
		
			if (!vFM.hasVideoData(v))
				vFM.saveVideoData(v, videoData.getInputStream());
		}	
		else {
			resp.sendError(404, "Video id does not exist");
		}
		
		VideoStatus videoStatus = new VideoStatus(VideoState.READY);
		return videoStatus;
	}
	
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
	public HttpServletResponse getData(@PathVariable("id") long id, HttpServletResponse resp) throws IOException
	{		
		if (!videoMap.containsKey(id)) {
			resp.sendError(404, "Video id does not exist");
			return resp;
		}

		Video v = videoMap.get(id);
		vFM = VideoFileManager.get();
		
		if (!vFM.hasVideoData(v)) {
			resp.sendError(404, "Video data does not exist " + vFM.getVideoPath(v));
			return resp;
		}	
	
		vFM.copyVideoData(v, resp.getOutputStream());
		return resp;
	}
	
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
	
}
