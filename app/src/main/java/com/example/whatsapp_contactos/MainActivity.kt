package com.example.whatsapp_contactos

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var contactAdapter: ContactAdapter
    private val contactList = mutableListOf<Contact>()
    private val filteredContactList = mutableListOf<Contact>() // Lista filtrada

    // Gerenciamento de permissão moderna
    private val requestContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadContacts()
        } else {
            Toast.makeText(this, "Permissão de contatos negada!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)

        // Inicializa RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter(filteredContactList) { contact ->
            sendMessageToWhatsApp(contact.phoneNumber)
        }
        recyclerView.adapter = contactAdapter

        // Solicita permissão para acessar os contatos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            requestContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        } else {
            loadContacts()
        }

        // Filtra os contatos conforme o usuário digita
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }
        })
    }

    // Carrega contatos da agenda do usuário
    private fun loadContacts() {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                contactList.add(Contact(name, number))
            }

            // Inicializa lista filtrada com todos os contatos
            filteredContactList.addAll(contactList)
            contactAdapter.notifyDataSetChanged()
        }
    }

    // Filtra a lista de contatos conforme o nome digitado
    private fun filterContacts(query: String) {
        filteredContactList.clear()

        // Se o campo de pesquisa estiver vazio, mostra todos os contatos
        if (query.isEmpty()) {
            filteredContactList.addAll(contactList)
        } else {
            // Caso contrário, filtra os contatos pelo nome
            val filtered = contactList.filter { contact ->
                contact.name.contains(query, ignoreCase = true)
            }
            filteredContactList.addAll(filtered)
        }

        contactAdapter.notifyDataSetChanged()
    }

    // Verifica se o WhatsApp está instalado e envia mensagem
    private fun sendMessageToWhatsApp(phoneNumber: String) {
        val uri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        intent.setPackage("com.whatsapp")
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "WhatsApp não está instalado.", Toast.LENGTH_SHORT).show()
        }
    }
}
