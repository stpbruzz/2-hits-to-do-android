package com.example.a2_hits_to_do_android

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class Task(var description: String, var flag: Boolean = false)
var TasksList = ArrayList<Task>()

class TaskAdapter(var context: Context, var tasks: ArrayList<Task>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return tasks.size
    }

    override fun getItem(position: Int): Any {
        return tasks[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = inflater.inflate(R.layout.list_template, parent, false)
        val holder = ViewHolder()
        holder.checkBox = view.findViewById(R.id.checkbox)
        holder.description = view.findViewById(R.id.task_description)
        holder.editButton = view.findViewById(R.id.edit_button)
        holder.deleteButton = view.findViewById(R.id.delete_button)

        val task = tasks[position]

        holder.description.setText(task.description)
        holder.description.isEnabled = false

        holder.editButton.setOnClickListener() {
            if (!holder.description.isEnabled) {
                holder.description.isEnabled = true
                holder.editButton.setImageResource(R.drawable.ic_save)
            } else {
                task.description = holder.description.text.toString()
                holder.description.isEnabled = false
                holder.editButton.setImageResource(R.drawable.ic_edit)
            }
        }

        holder.checkBox.isChecked = task.flag
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            task.flag = isChecked
            tasks.sortBy { it.flag }
            notifyDataSetChanged()
        }

        holder.deleteButton.setOnClickListener {
            tasks.removeAt(position)
            notifyDataSetChanged()
        }

        return view
    }

    private class ViewHolder {
        lateinit var checkBox: CheckBox
        lateinit var description: EditText
        lateinit var editButton: ImageButton
        lateinit var deleteButton: ImageButton
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val json = readFile(uri)
                if (json != null) {
                    convertJSON(json)
                }
            }
        }

        taskAdapter = TaskAdapter(this, TasksList)

        val listView = findViewById<ListView>(R.id.list)
        listView.adapter = taskAdapter

        findViewById<Button>(R.id.add_button).setOnClickListener {
            val taskDescription = findViewById<EditText>(R.id.task_description_input)
            TasksList.add(Task(taskDescription.text.toString()))
            taskAdapter.notifyDataSetChanged()
            taskDescription.setText("")
        }

        findViewById<Button>(R.id.download_button).setOnClickListener {
            val json = JSONArray()

            for (task in TasksList) {
                val taskObject = JSONObject()
                taskObject.put("description", task.description)
                taskObject.put("flag", task.flag)
                json.put(taskObject)
            }

            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var fileName = "to-do_save.json"
            var file = File(directory, fileName)
            var index = 1
            while (file.exists()) {
                fileName = "to-do_save($index).json"
                file = File(directory, fileName)
                index++
            }

            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(json.toString().toByteArray())
            fileOutputStream.close()

            Toast.makeText(this, "saved to download folder", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.upload_button).setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }
    }

    private fun readFile(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
    }

    private fun convertJSON(jsonString: String) {
        val jsonArray = JSONArray(jsonString)
        TasksList.clear()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val description = jsonObject.getString("description")
            val flag = jsonObject.getBoolean("flag")
            TasksList.add(Task(description, flag))
        }
        taskAdapter.notifyDataSetChanged()
        Toast.makeText(this, "uploaded", Toast.LENGTH_SHORT).show()
    }
}