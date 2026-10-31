# Cloudflare Tunnel — protected phone testing

Use Cloudflare Tunnel only for fictional demo data. **Do not publish the app without Cloudflare Access.** The `cloudflared tunnel --url http://localhost:3000` quick tunnel creates a public URL without email-based identity protection and is **not recommended** for this admin-enabled banking demo.

1. Install Cloudflare cloudflared (`winget install --id Cloudflare.cloudflared --exact`).
2. Create an account in Cloudflare Zero Trust and a **named tunnel** using the Cloudflare Dashboard. Assign a public hostname to the tunnel and set its service URL to `http://localhost:3000`.
3. Create a Cloudflare Access self-hosted application covering the entire hostname. Set allow rules to specific test email addresses only; use one-time PIN or your IdP to verify tester identity. Verify login from a private browser before sharing the hostname.
4. Build the React frontend into Spring Boot static resources using `scripts/build-single-origin.ps1`; start Spring Boot on port 3000.
5. Set `$env:SESSION_COOKIE_SECURE="true"` for HTTPS testing; configure Spring Boot trusted reverse-proxy forwarded headers if your specific tunnel topology requires them. Keep CSRF and Spring Security enabled.
6. Access `https://your-protected-hostname/` from iPhone/Samsung; install the PWA from the browser's share/install controls. Use JPEG/PNG upload or camera capture to simulate check deposits.
7. Keep MySQL (3306), MailHog SMTP (1025) and MailHog web (8025) bound to loopback only. Do not set Cloudflare routes for these ports.
8. Restrict or disable admin/demo endpoints when sharing outside trusted testers; Access is an additional perimeter, not a replacement for app login or MFA.
9. To stop the tunnel, stop the local `cloudflared` process or disable the tunnel in Zero Trust; stop the Spring Boot application when testing ends.

Official guides:
- https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/
- https://developers.cloudflare.com/cloudflare-one/applications/configure-apps/self-hosted-apps/

**Note:** `cloudflared tunnel ... --allowed-mail` is not a supported tunnel access-control shortcut. Use Cloudflare Access dashboard policies.
