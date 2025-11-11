# Project Requirement Proposal (PRP)
<!-- Adapted from https://github.com/Wirasm/PRPs-agentic-eng/tree/development/PRPs -->

You are a senior software engineer.
Use the information below to implement a new feature or improvement in this software project.

## Goal

**Feature Goal**: import of a new point of sale (POS) based on existing OpenStreetMap entry
**Deliverable**: feature, function class, using OSM 

**Success Definition**: succesfull creation of a POS based on any existing OpenStreetMap entry

## User Persona (if applicable)

**Target User**: students who live on campus 

**Use Case**: they want to add a new POS they found on OSM 

**User Journey**: has an OSM entry and wants to add it

**Pain Points Addressed**: no need of copy-pasting, typos, etc

## Why

- more POS means more fun
- should work with the existing 'add new POS' feature  
- solves problem of implementing POS because users can do it themselves 

## What

The feature to be implemented is the import of a new point of sale (POS) based on an existing
OpenStreetMap entry. As an example, you can look at the entry for “Rada Coffee & Rösterei”
in the old town. The corresponding OSM node is 5589879349 (Map, XML). The changelog of
the GitHub repository for this exercise sheet documents the changes that we implemented to
prepare for the addition of the new feature.

### Success Criteria

- [ ] can implement new POS based on OSM entry
- [ ] easy for the user to do so

## Documentation & References

MUST READ - Include the following information in your context window.

The `README.md` file at the root of the project contains setup instructions and example API calls.

This Java Spring Boot application is structured as a multi-module Maven project following the ports-and-adapters architectural pattern.
There are the following submodules:

`api` - Maven submodule for controller adapter.

`application` - Maven submodule for Spring Boot application, test data import, and system tests.

`data` - Maven submodule for data adapter.

`domain` - Maven submodule for domain model, main business logic, and ports.
