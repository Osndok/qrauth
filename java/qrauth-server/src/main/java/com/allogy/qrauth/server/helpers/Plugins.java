package com.allogy.qrauth.server.helpers;

import com.allogy.qrauth.server.services.impl.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by robert on 3/6/15.
 */
public
class Plugins
{
	public static synchronized <T>
	T get(Class<T> aClass, T _default)
	{
		if (cache.containsKey(aClass))
		{
			return (T)cache.get(aClass);
		}
		else
		{
			final
			String key=aClass.getName();

			final
			String className = Config.get().getProperty(key);

			log.info("{} -> {}", key, className);

			if (className==null)
			{
				cache.put(aClass, _default);
				return _default;
			}
			else
			{
				try
				{
					final
					Class<T> bClass=(Class<T>)Class.forName(className);

					T t=bClass.newInstance();

					cache.put(aClass, t);
					return t;
				}
				catch (Exception e)
				{
					log.error("bad config/class: {}", className, e);

					TOTALLY_HAPPY=false;

					cache.put(aClass, _default);
					return _default;
				}
			}
		}
	}

	public static boolean TOTALLY_HAPPY=true;

	private static final
	Logger log = LoggerFactory.getLogger(Plugins.class);

	private static final
	Map<Class, Object> cache = new WeakHashMap<Class, Object>();

}
