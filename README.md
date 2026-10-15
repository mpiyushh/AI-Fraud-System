# AI-Based Fraud Detection System

## Overview

The AI-Based Fraud Detection System is a machine learning-powered application
designed to identify potentially fraudulent financial transactions.

The system analyzes transaction-related features such as transaction amount,
time, location, user behavior, and device information to assess the likelihood
of fraud. It generates a fraud probability/risk score and can raise alerts
for suspicious transactions.

## Problem Statement

Financial fraud is a major challenge in digital transactions. Traditional
fraud detection methods may rely heavily on predefined rules and may not
effectively identify complex or evolving fraudulent patterns.

This project aims to develop an intelligent system that uses machine learning
to analyze transaction patterns and assist in identifying potentially
fraudulent activity.

## Proposed Solution

The system will collect transaction information through a user interface,
process and store the data using a Java-based backend and database, and send
relevant transaction features to a machine learning model.

The ML model will generate a fraud probability and risk score. The result
will then be stored and displayed to the user, with suspicious transactions
being flagged for further investigation.

## Key Features

- User and transaction management
- Transaction data storage
- Machine learning-based fraud prediction
- Fraud probability and risk scoring
- Suspicious transaction alerts
- Transaction history
- Database-backed data management
- Web-based user interface

## Technology Stack

### Frontend
- HTML
- CSS
- JavaScript

### Backend
- Java
- Java Servlets
- JDBC
- Maven
- Apache Tomcat

### Database
- Supabase

### Machine Learning
- Python
- Scikit-learn

### Version Control
- Git
- GitHub

## System Workflow

User
  ↓
Web Interface
  ↓
Java Servlet / Backend
  ↓
Database
  ↓
ML Fraud Detection Model
  ↓
Fraud Probability / Risk Score
  ↓
Fraud Alert
  ↓
Result displayed to User

## Team Members


### Krishnna Mudgal
**Team Leader – Machine Learning & Data Processing | Research & Documentation Lead**

**Responsibilities:**
- Dataset preparation and preprocessing
- Feature engineering and ML model development
- Model evaluation and fraud prediction
- Cybersecurity and fraud detection research
- Research on fraud detection techniques
- Project requirements documentation
- Test-case preparation
- Technical documentation and final report
- Presentation preparation


### Piyush Mishra
**Database, Backend & System Integration**

Responsibilities:
- Database architecture
- ER/schema design
- Models/entities
- JDBC connectivity
- Database queries
- Repository/data-access layer
- Indexing
- Data integrity
- Database testing
- Backend integration


### Shashank Chaturvedi
**Frontend & UI/UX**

Responsibilities:
- User interface
- Dashboard
- Transaction input forms
- Result visualization
- Frontend styling
- UI/UX design

### Rohit Ranjan


## Project Structure

```text
AI-Fraud-Detection-System/
│
├── frontend/
├── backend/
├── ml-model/
├── database/
├── tests/
├── docs/
└── README.md
