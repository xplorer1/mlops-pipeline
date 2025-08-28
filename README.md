# Customer Churn Analysis (Telco)

### Overview
This repository investigates customer churn for a telecommunications company using a structured data analysis and modeling workflow. It explores drivers of churn, builds baseline predictive models, and visualizes insights that can inform retention strategies.

### Objectives
- Understand overall churn rate and key drivers
- Engineer features suitable for modeling
- Train and evaluate baseline classification models
- Provide actionable insights for retention and product teams

### Dataset
- Source: Telco customer dataset (`Telco_customer_churn.xlsx`)
- Records: ~7,000 customers with demographics, services, account, and churn labels
- Target: `Churn Label` (binary)
- Example feature groups:
  - Customer profile: Senior Citizen, Partner, Dependents
  - Services: Phone Service, Internet Service, Streaming TV/Movies, Online Backup, Device Protection, Tech Support
  - Account: Tenure, Contract, Paperless Billing, Payment Method, Monthly/Total Charges

### Repository Contents
- `cugwuany_final_project.ipynb`: End‑to‑end analysis notebook (EDA → feature prep → modeling → evaluation)
- `Telco_customer_churn.xlsx`: Dataset used in the analysis
- `Customer Churn ML OPs Pipeline.png`: Output diagram of the MLOps pipeline described below
- `Pipeline Documentation.txt`, `Pipeline Detailed Analysis.txt`: Security, risk, and governance notes

### MLOps Pipeline (High‑Level)
- Data Sources: Internal customer data and optional third‑party data
- Data Validation & Anomaly Detection: Sanity checks, schema validation, and RBAC‑gated access
- Feature Preparation: Cleaning, categorical normalization (e.g., harmonizing "No internet service" → "No"), encoding, scaling
- Model Training: Baseline classifiers (e.g., Logistic Regression, Random Forest) with evaluation (accuracy, ROC‑AUC, classification report)
- Staging & Orchestration: Versioned artifacts, secure CI/CD, dependency hygiene
- Deployment: Customer churn prediction service (API)
- Monitoring: Performance, drift, and security monitoring with alerting; scheduled retraining

The image `Customer Churn ML OPs Pipeline.png` visualizes this flow and its security controls.

### Security Considerations (from pipeline docs)
- Data Poisoning: Validate external feeds, anomaly detection pre‑ingestion, HTTPS/TLS, signed feeds, RBAC
- Inference Attacks: API auth + rate limiting, limit probability granularity, optional differential privacy
- Model Theft: Throttling, restricted debugging info, monitoring unusual request patterns
- CI/CD Vulnerabilities: Least‑privilege access, secrets vault, static security scans, signed commits, secure builds
- Model Drift & Adversarial Inputs: Adversarial training, input validation in production, drift monitoring
- Insider Tampering: RBAC/MFA on data, immutable label archives, label QA & approval workflow
- Supply Chain Risks: Dependency scanning, strict version pinning and checksums, isolated builds, manual review of critical updates

### Responsible AI & Governance (from detailed analysis)
- Explainability: Provide feature importance (e.g., SHAP) and plain‑language rationales
- Drift Prevention: Monitor data/target distributions; schedule and/or trigger retraining
- Continuous Data: Automated pipelines and a repeatable feature computation process
- Human‑in‑the‑Loop: Confidence thresholds, review/approval for high‑impact actions
- Communication & UX: Clear confidence intervals and interpretation guidance for business users
- Data Minimization & Privacy: Collect only necessary data; anonymize; access controls; consent management
- GDPR/Data Rights: Support deletion, access, and lineage tracking with retraining protocols as needed
- Fairness: Regular bias audits across demographic segments; balanced evaluation

### Approach (Notebook)
1. Data loading and quality checks (types, missing values, outliers)
2. Exploratory analysis (overall churn, segment comparisons)
3. Feature preparation (normalization, encoding, scaling)
4. Baseline modeling (Logistic Regression, Random Forest) and evaluation (accuracy, ROC‑AUC, classification report)
5. Insights and recommendations (segments with higher churn propensity; services and contracts linked to churn)

### Getting Started
Prerequisites:
- Python 3.9+ recommended
- JupyterLab or Jupyter Notebook
- Common data stack (pandas, numpy, scikit‑learn, matplotlib, seaborn)

Quick start:
1. Open `cugwuany_final_project.ipynb` in Jupyter
2. Ensure `Telco_customer_churn.xlsx` is in the same directory
3. Run cells top‑to‑bottom to reproduce the analysis and figures

If any dependency is missing:
```bash
pip install pandas numpy scikit-learn matplotlib seaborn
```

### Results (Highlights)
- Baseline models provide reference ROC‑AUC and classification metrics
- Subscriptions like Online Security, Tech Support, Device Protection, and Online Backup correlate with reduced churn
- Demographic and contractual factors (e.g., senior citizen status, contract type, tenure) relate to churn propensity

### Roadmap Ideas
- Expand model search (e.g., XGBoost) with systematic hyperparameter tuning
- Add experiment tracking, model registry, and full pipeline automation
- Productionize API with observability, drift defenses, and rollbacks
- Integrate fairness and explainability reporting dashboards

### License
If you plan to use or extend this work, please check your organization’s licensing requirements and add a suitable license file.
