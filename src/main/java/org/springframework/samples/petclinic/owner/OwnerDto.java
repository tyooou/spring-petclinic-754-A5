package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OwnerDto {

	private Integer id;

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	@NotBlank
	private String address;

	@NotBlank
	private String city;

	@NotBlank
	@Pattern(regexp = "\\d{10}", message = "{telephone.invalid}")
	private String telephone;

	private List<PetDto> pets = new ArrayList<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public List<PetDto> getPets() {
		return pets;
	}

	public void setPets(List<PetDto> pets) {
		this.pets = pets;
	}

	public static class PetDto {

		private Integer id;

		private String name;

		private LocalDate birthDate;

		private String type;

		private List<VisitDto> visits = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public LocalDate getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(LocalDate birthDate) {
			this.birthDate = birthDate;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public List<VisitDto> getVisits() {
			return visits;
		}

		public void setVisits(List<VisitDto> visits) {
			this.visits = visits;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

	public static class VisitDto {

		private Integer id;

		private LocalDate date;

		private String description;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

}
