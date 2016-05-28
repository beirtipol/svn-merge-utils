This page is still W.I.P.

# svn-merge-utils

Java-based SVN Merge Utilities for assisting development teams with merge tracking


## Why does this project exist?

SVN has had built-in merge recording for a few years now. I've never encountered any other developers who actually make use of this information. This project provides some executable jars which can assist development teams in tracking missing merges and resolving corrupted merge information.


## How do I use it?

Firstly, your team should be familiar with SVN best practices for branching and merging [SVN Basic Merging](http://svnbook.red-bean.com/en/1.7/svn.branchmerge.basicmerging.html "SVN Basic Merging")

You should use the svn-merge-utils-mergechecker.jar to compare each of your branches to your trunk. You should run this at least on an hourly basis, perhaps using a 5 minute quiet time to avoid catching developers who commit to the branch just prior to the missing merges being checked.

Some best practices:
- Always perform the merge operation on a project at the root level of the project. SVN does not deal well with merges being performed at different levels of a project. Regardless of if you use Tortoise, subclipse, command line, 



