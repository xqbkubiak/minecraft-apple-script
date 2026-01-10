package pl.bkubiak.bkrynek.client.util;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import pl.bkubiak.bkrynek.core.config.ServersConfig;
import pl.bkubiak.bkrynek.core.util.RemoteAdConfig;
import pl.bkubiak.bkrynek.client.mixin.ServerListAccessor;

import java.util.List;
import java.util.Locale;

public final class ServerListPatcher {
    private ServerListPatcher() {
    }

    public static void injectOrMove(ServerList serverList) {
        // Disabled by user request
    }

    private static String normalize(String address) {
        if (address == null)
            return null;
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(":25565"))
            a = a.substring(0, a.length() - 6);
        return a;
    }

    private static void persist(ServerList serverList) {
        try {
            serverList.getClass().getMethod("saveFile").invoke(serverList);
            return;
        } catch (Throwable ignored) {
        }
        try {
            serverList.getClass().getMethod("save").invoke(serverList);
        } catch (Throwable ignored) {
        }
    }

    private static ServerInfo createServerInfo(String name, String address) {
        try {
            java.lang.reflect.Constructor<?>[] ctors = ServerInfo.class.getConstructors();
            for (java.lang.reflect.Constructor<?> c : ctors) {
                Class<?>[] pts = c.getParameterTypes();
                Object[] args = new Object[pts.length];
                int stringCount = 0;
                boolean unknown = false;
                for (int i = 0; i < pts.length; i++) {
                    Class<?> t = pts[i];
                    if (t == String.class) {
                        args[i] = (stringCount == 0) ? name : address;
                        stringCount++;
                    } else if (t == boolean.class || t == Boolean.class) {
                        args[i] = Boolean.FALSE;
                    } else if (t.isEnum()) {
                        Object[] constants = t.getEnumConstants();
                        if (constants != null && constants.length > 0) {
                            args[i] = constants[0];
                        } else {
                            unknown = true;
                            break;
                        }
                    } else {
                        try {
                            if ("net.minecraft.text.Text".equals(t.getName())) {
                                Class<?> textClass = Class.forName("net.minecraft.text.Text");
                                java.lang.reflect.Method ofMethod = textClass.getMethod("of", String.class);
                                args[i] = ofMethod.invoke(null, name);
                            } else {
                                unknown = true;
                                break;
                            }
                        } catch (Throwable ex) {
                            unknown = true;
                            break;
                        }
                    }
                }
                if (unknown)
                    continue;
                try {
                    Object inst = c.newInstance(args);
                    return (ServerInfo) inst;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
