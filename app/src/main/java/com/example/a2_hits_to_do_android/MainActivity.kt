package com.example.a2_hits_to_do_android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class Task(var description: String, var id: Int? = null, var flag: Boolean = false)
var TasksList = ArrayList<Task>()
val client = OkHttpClient()

class TaskAdapter(
    private var context: Context,
    private var tasks: ArrayList<Task>,
    private val activity: MainActivity) : BaseAdapter() {

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
        val view : View
        val holder : ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_template, parent, false)
            holder = ViewHolder()
            holder.checkBox = view.findViewById(R.id.checkbox)
            holder.description = view.findViewById(R.id.task_description)
            holder.editButton = view.findViewById(R.id.edit_button)
            holder.deleteButton = view.findViewById(R.id.delete_button)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val task = tasks[position]

        holder.description.setText(task.description)
        holder.description.isEnabled = false

        holder.editButton.setOnClickListener {
            if (!holder.description.isEnabled) {
                holder.description.isEnabled = true
                holder.editButton.setImageResource(R.drawable.ic_save)
            } else {
                task.description = holder.description.text.toString()
                holder.description.isEnabled = false
                holder.editButton.setImageResource(R.drawable.ic_edit)

                val id = tasks[position].id
                val url = "http://10.0.2.2:5112/api/ToDo/$id/description"
                val jsonObject = JSONObject()
                jsonObject.put("description", task.description)

                val mediaType = "application/json".toMediaTypeOrNull()
                val body = jsonObject.toString().toRequestBody(mediaType)

                val request = Request.Builder().url(url).patch(body).build()

                client.newCall(request).enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        (context as? AppCompatActivity)?.runOnUiThread {
                            notifyDataSetChanged()
                        }
                    }
                })
            }
        }

        holder.checkBox.isChecked = task.flag
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            task.flag = isChecked
            val id = tasks[position].id
            val url = "http://10.0.2.2:5112/api/ToDo/$id/flag"

            val request = Request.Builder().url(url).patch("".toRequestBody(null)).build()

            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    (context as? AppCompatActivity)?.runOnUiThread {
                        notifyDataSetChanged()
                    }
                }
            })
        }

        holder.deleteButton.setOnClickListener {
            val id = tasks[position].id
            val url = "http://10.0.2.2:5112/api/ToDo/$id"

            val request = Request.Builder().url(url).delete().build()

            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    (context as? AppCompatActivity)?.runOnUiThread {
                        tasks.removeAt(position)
                        notifyDataSetChanged()
                    }
                }
            })

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

        taskAdapter = TaskAdapter(this, TasksList, this)

        val listView = findViewById<ListView>(R.id.list)
        listView.adapter = taskAdapter

        findViewById<ImageButton>(R.id.add_button).setOnClickListener {
            val descriptionField = findViewById<EditText>(R.id.task_description_input)
            val jsonObject = JSONObject()
            jsonObject.put("description", descriptionField.text.toString())

            val mediaType = "application/json".toMediaTypeOrNull()
            val body = jsonObject.toString().toRequestBody(mediaType)

            val url = "http://10.0.2.2:5112/api/ToDo/"
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        TasksList.add(Task(descriptionField.text.toString()))
                        taskAdapter.notifyDataSetChanged()
                        descriptionField.setText("")
                    }
                }
            })
        }

        getData()

        findViewById<ImageButton>(R.id.update_button).setOnClickListener {
            getData()
        }
    }

    private fun getData() {
        val url = "http://10.0.2.2:5112/api/ToDo/"
        val request = Request.Builder().url(url).build()
        TasksList.clear()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val data = responseBody.string()
                    runOnUiThread {
                        parse(data)
                    }
                }
            }
        })
    }

    fun parse(data: String) {
        val jsonTasks = JSONArray(data)
        TasksList.clear()

        for (i in 0 until jsonTasks.length()) {
            val task = jsonTasks.getJSONObject(i)
            TasksList.add(Task(task.getString("description"), task.getInt("id"), task.getBoolean("flag")))
        }
        TasksList.sortBy { it.id }
        taskAdapter.notifyDataSetChanged()
    }
}