# 📚 Timetable App - Adding New Classes Guide

## 🎯 **Current Status**
- ✅ **A8 Class**: Complete and working timetable (Monday-Friday)
- ✅ **A1 & A2 Classes**: Basic structure with correct time format
- ✅ **Time Logic**: Fixed and working properly
- ✅ **Real-time Updates**: Classes update status every minute

## 🔧 **Issues Fixed**
1. **Time Format**: Changed from "12:20 - 01:20" to "12:20 - 13:20" (24-hour format)
2. **Time Parsing**: Enhanced to handle afternoon times correctly
3. **Real-time Updates**: Added automatic refresh every minute
4. **Class Status**: Now properly shows COMPLETED/UPCOMING/CURRENT

## 📝 **How to Add New Classes**

### **Step 1: Choose Your Class**
Decide which class you want to add (e.g., A3, A4, A5, etc.)

### **Step 2: Copy the Template**
Find the A8 schedule in the code and copy its structure:

```kotlin
"A8" -> listOf(
    ClassSchedule("9:00 - 10:00", "Lateral Thinking", "Room 301", "Mrs.M.Kanimozhi", "Theory"),
    ClassSchedule("10:00 - 11:00", "Data Structure", "Room 302", "Mrs.S.Lavanya", "Theory"),
    ClassSchedule("11:20 - 12:20", "OOP Lab", "Lab 3", "Mr.R.Karthikeyan", "Lab"),
    ClassSchedule("12:20 - 13:20", "OOP Lab", "Lab 3", "Mr.R.Karthikeyan", "Lab"),
    ClassSchedule("14:30 - 15:30", "Mathematics", "Room 303", "Dr.E.Ramesh Kumar", "Theory"),
    ClassSchedule("15:45 - 16:45", "R Programming", "Room 304", "Ms.J.Manivanan", "Theory")
)
```

### **Step 3: Replace with Your Class Details**
Change "A8" to your class name and update all the details:

```kotlin
"A3" -> listOf(
    ClassSchedule("9:00 - 10:00", "Your Subject 1", "Room XXX", "Faculty Name", "Theory"),
    ClassSchedule("10:00 - 11:00", "Your Subject 2", "Room XXX", "Faculty Name", "Theory"),
    ClassSchedule("11:20 - 12:20", "Your Lab 1", "Lab X", "Faculty Name", "Lab"),
    ClassSchedule("12:20 - 13:20", "Your Lab 1", "Lab X", "Faculty Name", "Lab"),
    ClassSchedule("14:30 - 15:30", "Your Subject 3", "Room XXX", "Faculty Name", "Theory"),
    ClassSchedule("15:45 - 16:45", "Your Subject 4", "Room XXX", "Faculty Name", "Theory")
)
```

### **Step 4: Repeat for All 5 Days**
You need to add this schedule to all 5 functions:
- `getMondaySchedule()`
- `getTuesdaySchedule()`
- `getWednesdaySchedule()`
- `getThursdaySchedule()`
- `getFridaySchedule()`

### **Step 5: Important Rules**
1. **Time Format**: Always use 24-hour format
   - ✅ Correct: "14:30 - 15:30"
   - ❌ Wrong: "2:30 - 3:30"

2. **Class Types**: Use "Theory", "Lab", or "Practical"

3. **Room Format**: Use "Room XXX" or "Lab X"

## 🎨 **Example: Adding A3 Class**

### **Monday Schedule:**
```kotlin
"A3" -> listOf(
    ClassSchedule("9:00 - 10:00", "Computer Networks", "Room 401", "Dr. Network", "Theory"),
    ClassSchedule("10:00 - 11:00", "Operating Systems", "Room 402", "Prof. OS", "Theory"),
    ClassSchedule("11:20 - 12:20", "Network Lab", "Lab 4", "Mr. NetworkLab", "Lab"),
    ClassSchedule("12:20 - 13:20", "Network Lab", "Lab 4", "Mr. NetworkLab", "Lab"),
    ClassSchedule("14:30 - 15:30", "Database Systems", "Room 403", "Dr. Database", "Theory"),
    ClassSchedule("15:45 - 16:45", "Web Development", "Room 404", "Ms. WebDev", "Theory")
)
```

### **Tuesday Schedule:**
```kotlin
"A3" -> listOf(
    ClassSchedule("9:00 - 10:00", "Software Engineering", "Room 401", "Dr. SE", "Theory"),
    ClassSchedule("10:00 - 11:00", "Data Structures", "Room 402", "Prof. DS", "Theory"),
    ClassSchedule("11:20 - 12:20", "Programming Lab", "Lab 4", "Mr. ProgLab", "Lab"),
    ClassSchedule("12:20 - 13:20", "Programming Lab", "Lab 4", "Mr. ProgLab", "Lab"),
    ClassSchedule("14:30 - 15:30", "Mathematics", "Room 403", "Dr. Math", "Theory"),
    ClassSchedule("15:45 - 16:45", "Artificial Intelligence", "Room 404", "Dr. AI", "Theory")
)
```

## 🔍 **Testing Your Changes**
1. Build and run the app
2. Select "School of Engineering" → "Year 2" → "Your New Class"
3. Check if the timetable displays correctly
4. Verify that class status updates properly (COMPLETED/UPCOMING/CURRENT)

## 📋 **Current Working Classes**
- ✅ **A8**: Complete timetable (Monday-Friday)
- ✅ **A1**: Basic structure (needs completion)
- ✅ **A2**: Basic structure (needs completion)
- ⏳ **A3-A10**: Need to be added

## 🚀 **Next Steps**
1. Complete A1 and A2 timetables
2. Add A3, A4, A5, etc. following the template
3. Test each class thoroughly
4. Add more departments if needed

## 💡 **Tips**
- Keep the same time slots for consistency
- Use realistic room numbers and faculty names
- Make sure lab sessions are 2 hours (e.g., 11:20-13:20)
- Test the app after adding each class

---
**Happy Coding! 🎉**
