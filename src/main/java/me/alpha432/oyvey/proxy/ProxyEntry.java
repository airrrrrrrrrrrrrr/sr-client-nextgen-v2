package me.alpha432.oyvey.proxy;

import io.netty.channel.ChannelHandler;
import me.alpha432.oyvey.proxy.netty.ProxyHandshakeHandler;

public class ProxyEntry {
   private ProxyEntry.Type type;
   private String host;
   private int port;
   private String username;
   private String password;

   public ProxyEntry(ProxyEntry.Type type, String host, int port, String username, String password) {
      this.type = type;
      this.host = host == null ? "" : host.trim();
      this.port = port;
      this.username = username == null ? "" : username;
      this.password = password == null ? "" : password;
   }

   public ProxyEntry.Type getType() {
      return this.type;
   }

   public void setType(ProxyEntry.Type type) {
      this.type = type;
   }

   public String getHost() {
      return this.host;
   }

   public void setHost(String host) {
      this.host = host == null ? "" : host.trim();
   }

   public int getPort() {
      return this.port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public String getUsername() {
      return this.username;
   }

   public void setUsername(String username) {
      this.username = username == null ? "" : username;
   }

   public String getPassword() {
      return this.password;
   }

   public void setPassword(String password) {
      this.password = password == null ? "" : password;
   }

   public boolean hasAuth() {
      return this.username != null && !this.username.isEmpty();
   }

   public boolean isValid() {
      return !this.host.isEmpty() && this.port > 0 && this.port <= 65535;
   }

   public ChannelHandler newHandler() {
      return new ProxyHandshakeHandler(this);
   }

   public String label() {
      return this.type.name().toLowerCase() + "  " + this.host + ":" + this.port + (this.hasAuth() ? "  *" : "");
   }

   public enum Type {
      HTTP,
      SOCKS4,
      SOCKS5;
   }
}
