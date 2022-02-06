Trawler is a BaaS system. Backend as a service. 


# BaaS

Generate REST and GraphQL frontend based on model defined in YAML that can be persisted in many different backend:
streams, document stores, RDMBS, and search engines. Add RBAC and ACL support via IAM system integration.


# Model


#### Model.yaml Example simple model
```yaml

module-namespace: Company


validation-rules:
  phone:
    min: 5
    max: 15
    regex: '\(?\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\>'

field-definitions:
  UUID:
    type: uuid 
  Name:
    type: string 
    # validation-rules: add validation rules here
    # length 
  PhoneNumber:
    type: string
    validation-rules: [phone]
    
  
defininitions:
  Employee:
    description: An employee of a company 
    fields:
      id:
        fieldDefinition: UUID 
        views: [admin] 
      firstName:
        fieldDefinition: Name 
        views: [*] # defaults to all views 
      lastName:
        fieldDefinition: Name
      homeNumber:
        fieldDefinition: PhoneNumber #assumes PhoneNumber but you can leave off #/fields/
      officeNumber:
        fieldDefinition: PhoneNumber
    associations:   
      department:
        defintion: Department
        
   Department:
       description: A department of a company 
       fields:
          id:
            field: UUID 
          name: 
            field: Name
            description: Name of department 
          manager: 
            field: Employee

       associations:
         manager:
           defintion : Employee
         employees:
           defintion: [Employee]
```

#### RDBMS-MIXIN.yaml Example mixin
```yaml 

module-namespace: MySQL 

Relationships:
  Employee:
    generate: 
        field: id
    manyToOne: 
        field: department
        model: Company/Department
        fk: departmentId # Can be written as Company/Employee/departmentId
        key: id # Can be written as Company/Department/id
        tags: [*] #load department info on all views 

  Depatment:
    generate: 
        field: id
    oneToMany: 
        field: employees
        model: Company/Employee 
        fk: Company/Employee/departmentId  # Company/Employee is optional and is assumed 
        key: Company/Department/id  # Company/Department/
        tags: [nested] #load employees info on only nested calls 
   
    oneToOne: 
        field: manager
        model: Company/Employee 
        fk: Company/Department/managerId 
        key: Company/Employee/id
        tags: [nested] #load employees info on only nested calls      

```

This is very rough.

#### DOCUMENT-MIXIN.yaml Example mixin
```yaml

module-namespace: Cassandra

Documents:
  Department:
    triggerModel: Department
    type: Primary 
    fields: [name, manager, id, employees]
  DepartmentShallow:
    triggerModel: Department
    type: View 
    fiels: [name, managerName:manager.name, id]
  
  Employee:
    triggerModel: Employee
    type: Primary 
    partitionKey: id 
    fields: [firstName, lastName, id, department]  
   
  EmployeeView:
    triggerModel: Employee
    type: View 
    partitionKey: [lastName, firstName]
    fields: [firstName, lastName, id, departmentName: department.name, departmentId, department.id, managerName: department.manager.name]    


```

This is very rough.

#### acl.yaml

```yaml 
module-namespace: BaseRoles 


roles:
  Admin:
    name: admin
    description: admin 
    active-tags : [admin]
    rulesets: [admin]
  
  User:
    name: user
    description: normal user
    rulesets: [user]
    properties:
      employeeId: Company/UUID
    
# Currently two types of resources Model (data model) and Path (URL path to front end).     
rulesets:
    admin:
      'Model/Company/Employee':
          effect: Allow
          resource: Model/Company/Employee # (default is to use the name as the resource)
          action: [Create, Read, Update, Delete]
      
      'Company/Department':
          effect: Allow
          actions: [Create, Read, Update, Delete]
      
    user: 
      'Company/Employee':
          effect: Allow #default is Allow
          actions: [Read, Update]
          conditions: 
            - 'principal.employeeId=this.id'
      
      'Company/Department':
          actions: [Read, Update]
          conditions: 
            [principal.employeeId=this.manager.id]
          
      
      'Company/Employee':
          actions: [Read]
          roles: [#roles:User]
      
      'Company/Department':
          actions: [Read]

```
