package io.melvinjones.weatherservice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController {

	private static final String helloTemplate = "Hello, %s!";
	private static final String hostNameTemplate = "The current hostname is %s";
	private final AtomicLong counter = new AtomicLong();

	@GetMapping("/hello-world")
	@ResponseBody
	public Greeting sayHello(@RequestParam(name="name", required=false, defaultValue="Stranger") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(helloTemplate, name));
	}

	@GetMapping("/hostname")
	@ResponseBody
	public Hostname sayHostname() {

		InetAddress ip = null;
		String hostname = null;
		try {
			ip = InetAddress.getLocalHost();
			hostname = ip.getHostName();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return new Hostname(counter.incrementAndGet(), String.format(hostNameTemplate, ip));
	}

}
