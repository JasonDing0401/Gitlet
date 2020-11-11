# Gitlet Design Document

**Name**: Jason Ding

## Classes and Data Structures

### Main

The class that deals with all the input commands and refers other classes for later use.

### Blob

The class that points to the file

### Branch

The class for users to checkout stages

### Commit

The class for users to commit file using trees

### History

The class that contains all the previous commitments.

### Stage

The class that helps store all stages

### Misc
Every Branch has a staging area.



## Algorithms

Initially, the user’s input will be passed to the `main` method. The `Main` class will catch any errors that may occur during execution, ensuring the **functionality** of the gitlet. Then, the `main` class will also call to use gitlet operations by executing the methods that correspond to each command. It will then use the corresponding class for execution. Every time, gitlet will track and modify the gitlet `repository` for every user command. A directory of commits and a directory of `blobs` will be stored inside the `.gitlet` folder. The `blobs` store the contents of their respective files with `trees` class as pointer to them. Both `commit` and `blob` will be saved via <u>serialization</u>. Every `Branch` will contain two string variables to hold its <u>name and HEAD commit ID</u>. `Repositories` will be initialized at the beginning before the execution.



## Persistence

In my `.gitlet` directory, I will mainly contain four subdirectories: `history`, `branch`, `blobs`, and `stages`. Now considering the persistence of my gitlet project, I will further explain the entire execution as my proof. In the `main` method, when dealing with the input command and file, the gitlet will first **initialize** a class object, or using the existing ones. Then, it will copy the file contens as a **Blob**, and **serialize** it to store in the `.gitlet`. Then, it will update the `HashMap` with the file name as a **key** and the blob **hash ID** as the **value**. When a commit command is given, it will first create a **new Commit object** with the **message**, taking its **hash ID** as a **instance variable**. Then, the `HashMap` of **Blobs** will copy the value from the current commit and overwrite the original `HashMap`. After that, the commit class will be serialized with fully considered **persistence**. 

***Caution**: All the contents are subjected to change due to the real programming of the gitlet.