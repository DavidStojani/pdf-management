# TODOs

## 2026-02-11
8. Improve backend exception handling: Examine how exceptions are handeled in the application. -- HIGH  ---DONE
   9. Improve frontend error handling + surface correct HTTP codes. -- HIGH     ---DONE

      3. Fix login/register behavior in backend (validation + responses). --MEDIUM  --- DONE

4. Test full flow; fix enrichment status mismatch (Ollama failure still ends as `ENRICHMENT_COMPLETED`). -- HIGH  ---DONE

2. Add a batch or a cronjob or similar, to see which documents are not 'ENRICHMENT_COMPLETED', and start a new thread to process them again, from the step where they failed -- DONE

---UI web---
6. UI--- Remove the div where this is: Search through your documents using a tag, keyword, or a sentence describing what you're looking for.
5. UI--- Add tag search in UI: right under the search bar the most repeated tags that in the DB should be shown there. if selected the documents with the tag should be shown. also there should be some pagination, and lazy loading so the flow is smoth and not slow.
5. UI--- Show document title (post-enrichment) in results panel.
6. UI--- Show page count near document name inside () and in a light grey 
7. Implement favourites feature end-to-end. -- UI+backend
---UI web---

---REFACTOR---
│
│ - Hardcoded AES key & ECB mode in AESCryptoUtil.java — needs dedicated security ticket                               │
│ - Hardcoded Ollama IP in WebClientConfig.java — needs infra/config review                                            │
│ - Splitting DocumentServiceImpl — architectural refactor, separate task                                              │
│ - Domain object immutability — broad changes to entity usage patterns                                                │
│ - Hardcoded OCR datapath with typo in TessOcrExtractionStrategyImpl.java — needs config review                       │
---REFACTOR


12. implement a audit level where 


10. Add observability stack (Grafana/Prometheus or similar). -- LOW
11. Implement CI/CD pipeline making use of synonlogy 218(NO DOCKER) apps and my ubuntu server. -- LOW
 
13. Research OpenSearch (for search backend). --LATER
13. Plan security infrastructure (HashiCorp Vault or similar). --LATER
14. Improve LLM prompt quality. --LATER
15. Explore RAG for “chat with docs”. --LATER
16. Ask Claude for app version feedback. --LATER
