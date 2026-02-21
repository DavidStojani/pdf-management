# Security Plan for Sensitive Document Management

This document outlines security measures and considerations for managing sensitive, encrypted documents, particularly focusing on database security and integrating search functionalities like OpenSearch/Elasticsearch.

---

### 1. Application-Level Encryption Assessment

**Current Approach:** Encrypting content before saving it to the database (application-level encryption).

**Security Assessment:**
*   **Strengths:**
    *   **Protects against Data-at-Rest Breaches:** Effectively renders data unintelligible if the database is compromised via raw dumps, backup theft, or direct server access, provided the encryption key remains secure.
    *   **Mitigates Insider Threats:** Safeguards sensitive information from unauthorized access by individuals with database access (e.g., DBAs, developers) who lack access to the application's encryption keys.
*   **Limitations:**
    *   **Application Compromise Risk:** An attacker who compromises the application server might gain access to the encryption key stored in memory, configuration files, or environment variables, thereby enabling decryption of the stored data.

**Conclusion:** Application-level encryption is a vital security layer, but it should be integrated into a comprehensive, multi-layered security strategy rather than being the sole protective measure.

---

### 2. Additional Security Measures

To enhance the overall security posture, consider implementing the following measures:

**2.1. Data Security:**
*   **Encryption in Transit (TLS/SSL):** Enforce TLS/SSL for all communication channels:
    *   Client to Application API.
    *   Application to Database.
    *   Application to Search Cluster (OpenSearch/Elasticsearch).
*   **Transparent Data Encryption (TDE):** Utilize database-native TDE features (available in most enterprise-grade databases like PostgreSQL, SQL Server). TDE encrypts the entire database files at the storage level, providing an additional layer of protection if the underlying storage or database files are directly accessed without authorization.
*   **Encrypted Backups:** All database backups, including incremental and full backups, must be encrypted. Ideally, use a separate key for backup encryption.

**2.2. Key Management (Crucial for Application-Level Encryption):**
*   **Secure Storage of Encryption Keys:** The application's encryption key must *not* be stored directly within the application code, standard configuration files, or easily accessible environment variables.
*   **Leverage Secret Management Systems:**
    *   **Cloud Key Management Services (KMS):** For cloud-hosted applications, utilize managed KMS offerings (e.g., AWS KMS, Google Cloud KMS, Azure Key Vault). These services allow applications to use encryption keys for cryptographic operations without directly exposing the key material to the application.
    *   **HashiCorp Vault:** An industry-standard open-source tool for centrally managing and securely accessing secrets, including encryption keys.
    *   **Hardware Security Modules (HSMs):** For the highest level of security assurance, consider HSMs to protect and manage cryptographic keys.

**2.3. Access Control:**
*   **Principle of Least Privilege:** Configure database users with the minimum necessary permissions. The application's database user should only have `SELECT`, `INSERT`, `UPDATE`, and `DELETE` rights on the specific tables it interacts with, rather than broad administrative privileges.
*   **Robust Authentication & Authorization:** Implement strong authentication mechanisms and fine-grained authorization checks within the application itself to control who can access specific documents and features.

**2.4. Infrastructure & Operations:**
*   **Network Segmentation:** Deploy the database, application, and search cluster in separate, isolated network segments (e.g., private subnets in a VPC) with strict firewall rules limiting ingress and egress traffic.
*   **Audit Logging:** Implement comprehensive audit logging for all access attempts, data modifications, and security-related events across the application, database, and search infrastructure.
*   **Regular Patching & Updates:** Maintain a strict patching schedule for all operating systems, libraries, and application dependencies to address known vulnerabilities promptly.

---

### 3. Implementing Search with Encrypted Content (OpenSearch/Elasticsearch)

You cannot perform direct, meaningful full-text search operations on strongly encrypted data. Search engines require access to the plaintext to build their indexes. To integrate OpenSearch/Elasticsearch while maintaining the security of your sensitive documents, a **Hybrid Search Model** is recommended.

**3.1. Hybrid Search Model Workflow:**

1.  **Document Ingestion:**
    *   A user uploads a sensitive document.
    *   Your application **encrypts the full content** and stores it securely in your primary database (e.g., PostgreSQL). This encrypted content is your "source of truth."
    *   Concurrently, your application takes the **plaintext version** of the document and sends it to your dedicated, highly-secured OpenSearch/Elasticsearch cluster for indexing.
    *   Both the primary database record and the search index entry are linked by a common `documentId`.

2.  **Search Operation:**
    *   A user initiates a search query through your application's API.
    *   Your application forwards this query to the OpenSearch/Elasticsearch cluster.
    *   The search cluster performs its full-text search on the plaintext index.
    *   Elasticsearch returns a list of matching **Document IDs** (and potentially short, highlighted snippets) to your application. It **does not** return the full sensitive content.

3.  **Document Viewing/Retrieval:**
    *   Your application presents the search results (e.g., "Document ID: 123 - Title: Report - Snippet: ...").
    *   When the user selects a document to view (e.g., clicks on Document ID 123), your application performs the following:
        *   It fetches the **encrypted content** corresponding to `documentId=123` from your primary database.
        *   It **decrypts** the content using the application's secure encryption key.
        *   The decrypted, full document content is then securely displayed to the authorized user.

**3.2. Security Implications & Measures for the Search Cluster:**

The critical takeaway from the Hybrid Search Model is that **your OpenSearch/Elasticsearch cluster now contains plaintext sensitive data**. This elevates its security importance considerably.

*   **Treat the Search Cluster as Highly Sensitive:** It must be secured with the same rigor (or even greater) as your primary database and application server.
*   **Network Isolation:** Deploy the search cluster in a private, isolated network segment (e.g., a dedicated private subnet). Use strict firewall rules to ensure it can only be accessed by your application servers (and potentially your indexing pipeline, if separate). Block all public internet access.
*   **Strong Access Control:**
    *   Configure robust authentication for the search cluster. Use its built-in security features (e.g., OpenSearch Security plugin) for role-based access control.
    *   The application's service account used to interact with the search cluster should have the least privilege necessary (e.g., permissions only for indexing and searching specific indices).
*   **Encryption at Rest:** Ensure the data stored on the disks of your search cluster nodes is encrypted (using disk encryption or cloud-provider managed encryption).
*   **Encryption in Transit:** Enforce TLS/SSL for all communication to and from the search cluster.
*   **Audit Logging:** Enable and regularly review audit logs for the search cluster to detect suspicious activity.

#### Alternative: Searchable Encryption (Advanced & Complex)

While possible in theory, pure "searchable encryption" is rarely used in practice for full-text search due to its complexity and limitations:

*   **Concept:** Techniques like "blind indexing" (e.g., hashing or tokenizing words before encryption) or more advanced cryptographic schemes allow for searching encrypted data without decrypting it fully.
*   **Limitations:**
    *   **Reduced Search Functionality:** Often supports only exact word matches, making fuzzy matching, relevance scoring, and complex queries (e.g., phrase search, proximity search) very difficult or impossible.
    *   **High Complexity:** Implementing these schemes correctly is cryptographically challenging and prone to errors.
    *   **Performance Overhead:** Can introduce significant performance penalties.
    *   **Not a Standard Feature:** Requires custom development or specialized libraries/plugins, as it's not natively supported by off-the-shelf search engines.

**Recommendation:** For most business applications, the **Hybrid Search Model** offers the best balance of security, search functionality, and implementation feasibility.

---
This plan provides a robust framework for securing your sensitive document management system while enabling powerful search capabilities.


---


Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
Plan: Append Self-Hosted Deployment Security Section to SECURITY_PLAN.md

Context

The user wants to save the home-network/self-hosting security plan discussed in chat into docs/SECURITY_PLAN.md. The existing file covers app-level
encryption and Elasticsearch hybrid search security well, but has no section on deployment architecture, VPN access, or Docker network isolation for a
self-hosted setup. We will append a new section rather than overwrite the existing content.

 ---
4. Self-Hosted Deployment Security (Home Network)

For personal use hosted on a home network, the following architecture provides a strong security posture without over-engineering.

4.1. Recommended Network Architecture

Internet
│
└── Synology NAS — VPN Server (OpenVPN or WireGuard, UDP 1194/51820)
│
└── Home LAN (192.168.x.x)
│
└── Reverse Proxy — port 443 only (Nginx Proxy Manager or Traefik)
│
├── pdf-inbound-api (Spring Boot) ─┐
├── PostgreSQL                      ├── Docker internal network (not LAN-exposed)
├── Elasticsearch                   │
└── Ollama ────────────────────────┘

Key principle: Only the reverse proxy is reachable from the LAN. All backend services communicate over a Docker internal network invisible to the rest of
the home network.

4.2. Docker Network Isolation

In docker-compose.yml, use an internal: true network for backend services:

networks:
internal:
internal: true   # no external routing; services only reachable by each other
proxy:
internal: false  # reverse proxy connects here

services:
pdf-api:
networks: [proxy, internal]
postgres:
networks: [internal]   # never exposed to LAN
elasticsearch:
networks: [internal]
ollama:
networks: [internal]

Only expose port 443 on the reverse proxy host. Remove any ports: mappings from PostgreSQL, Elasticsearch, and Ollama.

4.3. VPN for External Access

- OpenVPN on Synology (VPN Server package): mature, well-supported, solid choice
- WireGuard alternative: smaller codebase, faster handshake, easier to audit — preferred if Synology supports it
- Router firewall: allow only VPN port inbound (UDP 1194 for OpenVPN, UDP 51820 for WireGuard); block all other inbound traffic
- When away: connect VPN → access app exactly as if on home LAN

4.4. TLS / HTTPS

- Use Nginx Proxy Manager or Traefik as reverse proxy with TLS termination
- For internal-only use: generate a local CA with mkcert and trust it on your devices
- Alternatively, use a real domain pointing to an internal IP with Let's Encrypt DNS challenge (no public exposure needed)

4.5. Secrets Management

Ensure no secrets are hardcoded in application.yml or committed to git:

┌───────────────────────────┬────────────────────────────────────────────────┐
│          Secret           │                    Location                    │
├───────────────────────────┼────────────────────────────────────────────────┤
│ JWT signing key           │ .env file (not committed), injected as env var │
├───────────────────────────┼────────────────────────────────────────────────┤
│ DB password               │ .env file                                      │
├───────────────────────────┼────────────────────────────────────────────────┤
│ AES encryption key        │ .env file                                      │
├───────────────────────────┼────────────────────────────────────────────────┤
│ Elasticsearch credentials │ .env file                                      │
└───────────────────────────┴────────────────────────────────────────────────┘

Use spring.config.import=optional:file:.env[.properties] or Docker Compose env_file directive.

4.6. Application-Level Hardening (Pre-Go-Live Checklist)

- Rate limiting on /auth/login and /auth/register (Spring Security or Bucket4j)
- Short-lived JWT access tokens (15–30 min) with refresh token rotation
- HTTPS enforced — reject plain HTTP at reverse proxy level
- Principle of least privilege for DB user (no DDL rights in production)
- No stack traces in API error responses (production Spring profile)
- Elasticsearch accessible only from app container (no public Kibana)

4.7. Priority Order for Going Live

1. Docker internal network isolation — highest impact, quick Docker Compose change
2. Externalize all secrets to .env file, remove from application.yml
3. Reverse proxy + TLS — single HTTPS entry point
4. Rate limiting on auth endpoints — brute force protection
5. Synology VPN setup — enables secure external access

 ---
Verification

- Confirm PostgreSQL port is NOT reachable from outside the Docker network: nc -zv <host-ip> 5432 should fail
- Confirm app is reachable over HTTPS on port 443
- Confirm VPN connection allows access to the app from outside the LAN
- Run existing unit and integration tests: mvn test